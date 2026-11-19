export default function ChangeNotice({ message }) {
  return message ? <div className="notice" role="status">{message}</div> : null;
}
