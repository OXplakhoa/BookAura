export function normalizeIsbn(raw: string): string {
  return raw.replace(/[\s-]/g, "").toUpperCase();
}

export function isValidIsbn(raw: string): boolean {
  const isbn = normalizeIsbn(raw);
  if (/^\d{9}[\dX]$/.test(isbn)) {
    const sum = Array.from(isbn).reduce((total, character, index) =>
      total + (character === "X" ? 10 : Number(character)) * (10 - index), 0);
    return sum % 11 === 0;
  }
  if (/^\d{13}$/.test(isbn)) {
    const sum = Array.from(isbn.slice(0, 12)).reduce((total, character, index) =>
      total + Number(character) * (index % 2 === 0 ? 1 : 3), 0);
    return (10 - sum % 10) % 10 === Number(isbn[12]);
  }
  return false;
}
