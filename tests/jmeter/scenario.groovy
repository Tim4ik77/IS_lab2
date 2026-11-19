import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

// Each JMeter thread represents one authenticated user. No roles/owner IDs are client supplied.
def base = props.getProperty('baseUrl', 'http://127.0.0.1:8080') + '/api'
def users = ['admin', 'admin2', 'user', 'user2']
def index = ctx.getThreadNum()
def user = users[index % users.size()]
def phase = Parameters.trim()
def call = { String method, String path, Object body = null, Object version = null, String asUser = user, boolean multipart = false ->
    def connection = new URL(base + path).openConnection()
    connection.requestMethod = method
    connection.connectTimeout = 10000
    connection.readTimeout = 60000
    def password = props.getProperty(asUser + 'Password', asUser + '-lab2')
    connection.setRequestProperty('Authorization', 'Basic ' + (asUser + ':' + password).getBytes('UTF-8').encodeBase64().toString())
    if (version != null) connection.setRequestProperty('If-Match', version.toString())
    if (body != null) {
        connection.doOutput = true
        def payload = JsonOutput.toJson(body)
        if (multipart) {
            def boundary = 'jmeter-' + UUID.randomUUID()
            connection.setRequestProperty('Content-Type', 'multipart/form-data; boundary=' + boundary)
            payload = "--${boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"load.json\"\r\nContent-Type: application/json\r\n\r\n${payload}\r\n--${boundary}--\r\n"
        } else connection.setRequestProperty('Content-Type', 'application/json')
        connection.outputStream.withCloseable { it.write(payload.getBytes('UTF-8')) }
    }
    int code = connection.responseCode
    def stream = code >= 400 ? connection.errorStream : connection.inputStream
    def text = stream == null ? '' : stream.withCloseable { it.getText('UTF-8') }
    connection.disconnect()
    // Eager maps are safe to publish to other threads; JsonSlurper's LazyMap is not.
    [code: code, data: text ? new JsonSlurperClassic().parseText(text) : null]
}
def require = { response, expected ->
    assert response.code in expected : "Expected ${expected}, got ${response}"
    response.data
}
// Retry only independent operations after an explicit 409 (a rolled-back transaction).
// Never retry the shared-version/unique-name races: their losers must remain visible.
def retry = { operation ->
    def response = operation()
    for (int attempt=0; response.code == 409 && attempt<5; attempt++) {
        props.get('lab.retries').add([phase:phase,user:user,code:response.code])
        Thread.sleep(100L * (attempt+1) + index * 43L)
        response = operation()
    }
    response
}
def ticketBody = { t ->
    [name:t.name, coordinatesId:t.coordinates.id, personId:t.person?.id, eventId:t.event.id,
     venueId:t.venue.id, price:t.price, type:t.type, discount:t.discount, number:t.number]
}
def row = { String name, String eventName, String venueName ->
    [name:name, coordinates:[x:10, y:20], person:[eyeColor:'GREEN', hairColor:null, weight:70,
     location:[x:1, y:2, z:3, name:name]], event:[name:eventName, ticketsCount:100, eventType:'CONCERT'],
     venue:[name:venueName, capacity:200, type:'LOFT'], price:100, type:'USUAL', discount:10, number:1]
}
boolean synchronizedPhase = false
try {
    if (phase == 'setup') {
        def run = 'jmeter-' + UUID.randomUUID().toString()
        int rounds = props.getProperty('rounds', '5').toInteger()
        props.put('lab.run', run)
        props.put('lab.barrier', new CyclicBarrier(4))
        props.put('lab.results', new ConcurrentLinkedQueue())
        props.put('lab.retries', new ConcurrentLinkedQueue())
        def fixtures = []
        def event = require(call('POST', '/references/events', [name:run+'-base', ticketsCount:100, eventType:'CONCERT']), [201])
        def venue = require(call('POST', '/references/venues', [name:run+'-base', capacity:200, type:'LOFT']), [201])
        def coordinates = require(call('POST', '/references/coordinates', [x:10,y:20]), [201])
        def body = [name:run, coordinatesId:coordinates.id, personId:null, eventId:event.id, venueId:venue.id,
                    price:100, type:'USUAL', discount:10, number:1]
        for (int n=0; n<rounds; n++) {
            fixtures << ['update','delete','mixed'].collectEntries { kind ->
                [(kind): require(call('POST', '/tickets', body + [name:run+'-'+n+'-'+kind]), [201])]
            }
        }
        props.put('lab.fixtures', fixtures)
        props.put('lab.body', body)
        props.put('lab.event', event)
        props.put('lab.venue', venue)
        SampleResult.setResponseData('Fixtures ready: ' + run, 'UTF-8')
    } else if (phase == 'verify') {
        def results = props.get('lab.results').toList()
        def groups = results.groupBy { it.round + ':' + it.phase }
        int rounds = props.getProperty('rounds', '5').toInteger()
        assert groups.size() == rounds * 10 : "Missing phases: ${groups.keySet()}"
        groups.each { key, values ->
            assert values.size() == 4 : "Missing users: ${key}"
            def codes = values.collect { it.code }
            if (values[0].phase in ['unique-event','unique-venue','update','delete','mixed','import-race']) {
                assert codes.count { it in [200,201,204] } == 1 : "${key}: ${codes}"
            }
        }
        def events = require(call('GET','/references/events'), [200])
        def venues = require(call('GET','/references/venues'), [200])
        def run = props.get('lab.run')
        for (int n=1; n<=rounds; n++) {
            for (kind in ['event','import-race']) assert events.count { it.name == run+'-'+n+'-'+kind } == 1
            assert venues.count { it.name == run+'-'+n+'-venue' } == 1
            def fixture = props.get('lab.fixtures')[n-1]
            require(call('GET','/tickets/'+fixture.delete.id), [404])
            def changed = require(call('GET','/tickets/'+fixture.update.id), [200])
            assert changed.version.toLong() == fixture.update.version.toLong()+1
            def mixed = call('GET','/tickets/'+fixture.mixed.id)
            assert mixed.code == 404 || (mixed.code == 200 && mixed.data.version.toLong() == fixture.mixed.version.toLong()+1)
        }
        def out = new File(props.getProperty('outDir','tests/results'))
        out.mkdirs()
        new File(out,'jmeter-summary.json').setText(JsonOutput.prettyPrint(JsonOutput.toJson([run:run,rounds:rounds,users:users,retries:props.get('lab.retries').toList(),results:results])), 'UTF-8')
        SampleResult.setResponseData('All ' + results.size() + ' concurrent scenario samples verified', 'UTF-8')
    } else {
        int round = vars.getIteration()
        def barrier = props.get('lab.barrier')
        def name = props.get('lab.run') + '-' + round
        def fixture = props.get('lab.fixtures')[round-1]
        barrier.await(60, TimeUnit.SECONDS)
        synchronizedPhase = true
        def response
        switch (phase) {
            case 'unique-event':
                response = call('POST','/references/events',[name:name+'-event',ticketsCount:100,eventType:'CONCERT'])
                require(response,[201,409]); break
            case 'unique-venue':
                response = call('POST','/references/venues',[name:name+'-venue',capacity:200,type:'LOFT'])
                require(response,[201,409]); break
            case 'update':
                response = call('PUT','/tickets/'+fixture.update.id,ticketBody(fixture.update)+[price:200+index],fixture.update.version)
                require(response,[200,409]); break
            case 'delete':
                response = call('DELETE','/tickets/'+fixture.delete.id,null,fixture.delete.version)
                require(response,[204,404,409]); break
            case 'mixed':
                response = index % 2 == 0 ? call('PUT','/tickets/'+fixture.mixed.id,ticketBody(fixture.mixed)+[price:300+index],fixture.mixed.version)
                    : call('DELETE','/tickets/'+fixture.mixed.id,null,fixture.mixed.version)
                require(response,[200,204,404,409]); break
            case 'crud':
                def own = require(retry { call('POST','/tickets',props.get('lab.body')+[name:name+'-own-'+index]) },[201])
                own = require(retry { call('PUT','/tickets/'+own.id,ticketBody(own)+[price:123],own.version) },[200])
                response = retry { call('DELETE','/tickets/'+own.id,null,own.version) }
                require(response,[204]); break
            case 'import-success':
                def input = row(name+'-import-'+index,props.get('lab.event').name,props.get('lab.venue').name)
                response = retry { call('POST','/imports',[input],null,user,true) }
                def op = require(response,[201])
                assert op.status == 'SUCCESS' && op.addedCount == 1 && op.username == user
                break
            case 'import-rollback':
                def input = row(name+'-rollback-'+index,name+'-rollback-event-'+index,name+'-rollback-venue-'+index)
                def invalid = row(name+'-invalid-'+index,name+'-invalid-event-'+index,name+'-invalid-venue-'+index)
                invalid.person.location.x = null
                response = retry { call('POST','/imports',[input,invalid],null,user,true) }
                def op = require(response,[400])
                assert op.status == 'FAILED' && op.addedCount == null
                assert require(call('GET','/tickets?name='+input.name),[200]).total.toLong() == 0
                assert !require(call('GET','/references/events'),[200]).any { it.name == input.event.name }
                assert !require(call('GET','/references/venues'),[200]).any { it.name == input.venue.name }
                break
            case 'import-race':
                if (index % 2 == 0) {
                    response = call('POST','/references/events',[name:name+'-import-race',ticketsCount:500+index,eventType:'CONCERT'])
                } else {
                    def input = row(name+'-race-ticket-'+index,name+'-import-race',props.get('lab.venue').name)
                    input.event.ticketsCount = 500+index
                    response = call('POST','/imports',[input],null,user,true)
                }
                require(response,[201,409]); break
            case 'history':
                response = call('GET','/imports?size=100')
                def history = require(response,[200]).items
                assert history
                if (user.startsWith('user')) assert history.every { it.username == user }
                else assert history.any { it.username.startsWith('user') }
                assert history.every { it.status == 'SUCCESS' ? it.addedCount != null : it.addedCount == null }
                break
            default: throw new IllegalArgumentException(phase)
        }
        props.get('lab.results').add([round:round,phase:phase,user:user,code:response.code])
        SampleResult.setResponseData(JsonOutput.toJson(response), 'UTF-8')
    }
    SampleResult.setSuccessful(true)
    SampleResult.setResponseCode('200')
} catch (Throwable e) {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseCode('500')
    SampleResult.setResponseMessage(e.toString())
    SampleResult.setResponseData(e.toString(), 'UTF-8')
    log.error('Scenario failed: ' + phase, e)
} finally {
    // A failed assertion must not shift the remaining threads into different phases.
    if (synchronizedPhase) props.get('lab.barrier').await(60, TimeUnit.SECONDS)
}
