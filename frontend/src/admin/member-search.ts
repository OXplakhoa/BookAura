export function toBackendMemberDate(value: string): string | undefined {
  return value ? value.replaceAll("-", "/") : undefined;
}
