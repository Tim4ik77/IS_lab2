import { useEffect, useState } from 'react';

const currentRoute = () => window.location.hash.slice(1) || '/tickets';

export default function useHashRoute() {
  const [route, setRoute] = useState(currentRoute);
  useEffect(() => {
    const onChange = () => setRoute(currentRoute());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}
