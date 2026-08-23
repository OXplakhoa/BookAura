export function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) return null;
  return <p id={id} className="mt-1.5 text-sm font-medium text-danger" role="alert">{message}</p>;
}
