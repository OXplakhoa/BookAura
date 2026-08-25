import * as THREE from "three";
import type { AuraRecommendation } from "./aura-api";
import type { ArcaneAtmosphere } from "./arcane-atmosphere";

const COVER_COLORS = ["#123f3a", "#273b58", "#603640", "#59452b", "#3f3655", "#314938", "#6a3d2e"];

function hash(value: string): number {
  return Array.from(value).reduce((total, character) => ((total << 5) - total + character.charCodeAt(0)) | 0, 17);
}

function seededRandom(seed: number): () => number {
  let state = Math.abs(seed) || 1;
  return () => {
    state = (state * 16807) % 2147483647;
    return (state - 1) / 2147483646;
  };
}

function wrapText(context: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
  const words = text.trim().split(/\s+/);
  const lines: string[] = [];
  let line = "";
  for (const word of words) {
    const candidate = line ? `${line} ${word}` : word;
    if (line && context.measureText(candidate).width > maxWidth) {
      lines.push(line);
      line = word;
    } else {
      line = candidate;
    }
  }
  if (line) lines.push(line);
  return lines.slice(0, 5);
}

function drawCorner(context: CanvasRenderingContext2D, x: number, y: number, flipX: number, flipY: number, accent: string) {
  context.save();
  context.translate(x, y);
  context.scale(flipX, flipY);
  context.strokeStyle = accent;
  context.lineWidth = 3;
  context.beginPath();
  context.moveTo(0, 45);
  context.lineTo(0, 0);
  context.lineTo(45, 0);
  context.moveTo(10, 34);
  context.quadraticCurveTo(10, 10, 34, 10);
  context.stroke();
  context.restore();
}

export function createArcaneBookTexture(book: AuraRecommendation, rank: number, atmosphere: ArcaneAtmosphere): THREE.CanvasTexture {
  const canvas = document.createElement("canvas");
  canvas.width = 512;
  canvas.height = 720;
  const context = canvas.getContext("2d");
  if (!context) throw new Error("Canvas 2D context is unavailable for procedural book covers");

  const seed = hash(`${book.bookId}:${book.title}`);
  const random = seededRandom(seed);
  const cover = COVER_COLORS[Math.abs(seed) % COVER_COLORS.length];
  const accent = atmosphere.accent;

  context.fillStyle = cover;
  context.fillRect(0, 0, canvas.width, canvas.height);

  // Fine deterministic cloth grain keeps the cover tactile without external image assets.
  context.globalAlpha = 0.09;
  context.strokeStyle = "#fffdf8";
  context.lineWidth = 1;
  for (let index = 0; index < 180; index += 1) {
    const x = random() * canvas.width;
    const y = random() * canvas.height;
    context.beginPath();
    context.moveTo(x, y);
    context.lineTo(x + random() * 24 - 12, y + random() * 8 - 4);
    context.stroke();
  }
  context.globalAlpha = 1;

  context.strokeStyle = accent;
  context.lineWidth = 4;
  context.strokeRect(27, 27, 458, 666);
  context.lineWidth = 1.5;
  context.strokeRect(38, 38, 436, 644);
  drawCorner(context, 48, 48, 1, 1, accent);
  drawCorner(context, 464, 48, -1, 1, accent);
  drawCorner(context, 48, 672, 1, -1, accent);
  drawCorner(context, 464, 672, -1, -1, accent);

  context.save();
  context.translate(256, 145);
  context.strokeStyle = accent;
  context.lineWidth = 3;
  context.beginPath();
  context.arc(0, 0, 45, 0, Math.PI * 2);
  context.stroke();
  context.rotate(Math.PI / 4);
  context.strokeRect(-24, -24, 48, 48);
  context.rotate(-Math.PI / 4);
  context.font = "30px Georgia, serif";
  context.fillStyle = accent;
  context.textAlign = "center";
  context.textBaseline = "middle";
  context.fillText("✦", 0, 1);
  context.restore();

  context.fillStyle = "#fffdf8";
  context.textAlign = "center";
  context.textBaseline = "middle";
  context.font = "700 48px Georgia, serif";
  const titleLines = wrapText(context, book.title, 380);
  const titleStart = 330 - ((titleLines.length - 1) * 31);
  titleLines.forEach((line, index) => context.fillText(line, 256, titleStart + index * 62));

  context.strokeStyle = accent;
  context.lineWidth = 2;
  context.beginPath();
  context.moveTo(146, 520);
  context.lineTo(366, 520);
  context.stroke();

  context.fillStyle = "rgba(255, 253, 248, 0.82)";
  context.font = "22px Georgia, serif";
  const author = (book.authors[0] ?? "BookAura Collection").toUpperCase();
  context.fillText(author.length > 34 ? `${author.slice(0, 32)}…` : author, 256, 565);

  context.fillStyle = accent;
  context.font = "700 18px Arial, sans-serif";
  context.letterSpacing = "4px";
  context.fillText(`OPUS 0${rank}`, 256, 635);

  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.anisotropy = 8;
  texture.needsUpdate = true;
  return texture;
}
