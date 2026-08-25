import { Canvas, type ThreeEvent, useFrame } from "@react-three/fiber";
import { useEffect, useMemo, useRef } from "react";
import * as THREE from "three";
import type { AuraRecommendation } from "./aura-api";
import type { ArcaneAtmosphere } from "./arcane-atmosphere";
import { createArcaneBookTexture } from "./arcane-book-texture";

interface ArcaneShelfCanvasProps {
  books: AuraRecommendation[];
  atmosphere: ArcaneAtmosphere;
  activeIndex: number;
  mobile: boolean;
  onActive: (index: number) => void;
  onOpen: (bookId: string) => void;
}

interface BookPlacement {
  position: [number, number, number];
  rotation: [number, number, number];
  scale: number;
  featured: boolean;
}

const DESKTOP_PLACEMENTS: BookPlacement[] = [
  { position: [-2.75, 1.48, 0.18], rotation: [0, 0.1, 0.01], scale: 1, featured: true },
  { position: [0, 1.5, 0.34], rotation: [0, -0.03, -0.01], scale: 1.06, featured: true },
  { position: [2.75, 1.46, 0.16], rotation: [0, -0.11, 0.01], scale: 0.98, featured: true },
  { position: [-2.78, -1.34, 0.04], rotation: [0, 0.38, 0], scale: 0.88, featured: false },
  { position: [0, -1.32, 0.12], rotation: [0, -0.22, 0.02], scale: 0.9, featured: false },
  { position: [2.78, -1.36, 0.02], rotation: [0, -0.4, -0.01], scale: 0.87, featured: false },
];

export function ArcaneShelfCanvas({ books, atmosphere, activeIndex, mobile, onActive, onOpen }: ArcaneShelfCanvasProps) {
  return (
    <Canvas
      shadows
      dpr={[1, 1.7]}
      camera={{ fov: mobile ? 43 : 36, near: 0.1, far: 60, position: mobile ? [0, 0.15, 8.8] : [0, 0.25, 10.8] }}
      gl={{ antialias: true, alpha: false, powerPreference: "high-performance" }}
      fallback={<p className="arcane-opus__canvas-error">The enchanted shelf could not awaken.</p>}
      onCreated={({ gl }) => {
        gl.setClearColor(atmosphere.background);
        gl.toneMapping = THREE.ACESFilmicToneMapping;
        gl.toneMappingExposure = 1.08;
      }}
    >
      <ArcaneScene books={books} atmosphere={atmosphere} activeIndex={activeIndex} mobile={mobile} onActive={onActive} onOpen={onOpen} />
    </Canvas>
  );
}

function ArcaneScene({ books, atmosphere, activeIndex, mobile, onActive, onOpen }: ArcaneShelfCanvasProps) {
  const rig = useRef<THREE.Group>(null);
  const activePlacement = mobile
    ? { position: [0, 0, 0.8] as [number, number, number] }
    : DESKTOP_PLACEMENTS[activeIndex] ?? DESKTOP_PLACEMENTS[0];

  useFrame((state, delta) => {
    if (!rig.current) return;
    const ease = 1 - Math.exp(-delta * 2.2);
    const targetY = mobile ? 0 : state.pointer.x * 0.035;
    rig.current.rotation.y = THREE.MathUtils.lerp(rig.current.rotation.y, state.pointer.x * (mobile ? 0.025 : 0.065), ease);
    rig.current.rotation.x = THREE.MathUtils.lerp(rig.current.rotation.x, -state.pointer.y * (mobile ? 0.012 : 0.025), ease);
    rig.current.position.y = THREE.MathUtils.lerp(rig.current.position.y, targetY, ease);
    state.camera.position.x = THREE.MathUtils.lerp(state.camera.position.x, state.pointer.x * (mobile ? 0.12 : 0.38), ease);
    state.camera.position.y = THREE.MathUtils.lerp(state.camera.position.y, 0.25 + state.pointer.y * (mobile ? 0.04 : 0.18), ease);
    state.camera.lookAt(0, mobile ? 0 : -0.15, 0);
  });

  return (
    <>
      <fog attach="fog" args={[atmosphere.background, 9, 19]} />
      <ambientLight intensity={0.42} color="#8b7a68" />
      <hemisphereLight args={[atmosphere.auraSoft, atmosphere.woodDark, 0.55]} />
      <directionalLight castShadow color="#f2d9aa" intensity={1.55} position={[3.5, 6.5, 7]} shadow-mapSize-width={1024} shadow-mapSize-height={1024} />
      <pointLight color={atmosphere.aura} intensity={26} distance={7} decay={2} position={[activePlacement.position[0], activePlacement.position[1] + 0.4, 2.4]} />

      <group ref={rig} scale={mobile ? 0.88 : 1}>
        <ArcaneArchitecture atmosphere={atmosphere} mobile={mobile} />
        <ArcaneSigil atmosphere={atmosphere} mobile={mobile} />
        <FloatingMotes color={atmosphere.aura} mobile={mobile} />
        {books.slice(0, 6).map((book, index) => (
          <ArcaneBook
            key={book.bookId}
            book={book}
            rank={index + 1}
            index={index}
            atmosphere={atmosphere}
            active={index === activeIndex}
            activeIndex={activeIndex}
            mobile={mobile}
            onActive={onActive}
            onOpen={onOpen}
          />
        ))}
      </group>
    </>
  );
}

function ArcaneArchitecture({ atmosphere, mobile }: { atmosphere: ArcaneAtmosphere; mobile: boolean }) {
  return (
    <group>
      <mesh receiveShadow position={[0, 0, -1.35]}>
        <boxGeometry args={[9.7, 6.6, 0.28]} />
        <meshStandardMaterial color={atmosphere.woodDark} roughness={0.92} />
      </mesh>
      <mesh receiveShadow position={[0, -3.25, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[14, 9]} />
        <meshStandardMaterial color="#0b0908" roughness={0.95} />
      </mesh>

      {!mobile && (
        <>
          <WoodBeam position={[-4.65, 0, -0.25]} scale={[0.28, 6.75, 0.48]} color={atmosphere.wood} />
          <WoodBeam position={[4.65, 0, -0.25]} scale={[0.28, 6.75, 0.48]} color={atmosphere.wood} />
          <WoodBeam position={[0, 3.25, -0.25]} scale={[9.55, 0.28, 0.48]} color={atmosphere.wood} />
          <WoodBeam position={[0, 0.05, -0.08]} scale={[9.3, 0.24, 0.78]} color={atmosphere.wood} />
          <WoodBeam position={[0, -2.55, -0.08]} scale={[9.3, 0.3, 0.82]} color={atmosphere.wood} />
          <BrassSconce x={-4.05} color={atmosphere.aura} />
          <BrassSconce x={4.05} color={atmosphere.aura} />
        </>
      )}

      {mobile && (
        <>
          <WoodBeam position={[0, -1.55, -0.08]} scale={[8.6, 0.3, 0.82]} color={atmosphere.wood} />
          <WoodBeam position={[0, 2.25, -0.25]} scale={[8.8, 0.22, 0.4]} color={atmosphere.wood} />
        </>
      )}
    </group>
  );
}

function WoodBeam({ position, scale, color }: { position: [number, number, number]; scale: [number, number, number]; color: string }) {
  return (
    <mesh castShadow receiveShadow position={position}>
      <boxGeometry args={scale} />
      <meshStandardMaterial color={color} roughness={0.7} metalness={0.04} />
    </mesh>
  );
}

function BrassSconce({ x, color }: { x: number; color: string }) {
  return (
    <group position={[x, 1.25, -0.75]}>
      <mesh rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.08, 0.12, 0.75, 16]} />
        <meshStandardMaterial color="#9d7a35" metalness={0.8} roughness={0.28} />
      </mesh>
      <mesh position={[0, 0.42, 0]}>
        <sphereGeometry args={[0.13, 20, 20]} />
        <meshStandardMaterial color={color} emissive={color} emissiveIntensity={3.5} />
      </mesh>
      <pointLight color={color} intensity={8} distance={3.5} decay={2} position={[0, 0.42, 0.3]} />
    </group>
  );
}

function ArcaneSigil({ atmosphere, mobile }: { atmosphere: ArcaneAtmosphere; mobile: boolean }) {
  const group = useRef<THREE.Group>(null);
  useFrame((_, delta) => {
    if (group.current) group.current.rotation.z += delta * 0.025;
  });
  return (
    <group ref={group} position={[0, mobile ? 0.25 : 0.65, -1.12]} scale={mobile ? 0.72 : 1}>
      <mesh>
        <torusGeometry args={[2.15, 0.018, 8, 96]} />
        <meshStandardMaterial color={atmosphere.aura} emissive={atmosphere.aura} emissiveIntensity={1.8} transparent opacity={0.3} />
      </mesh>
      <mesh rotation={[0, 0, Math.PI / 4]}>
        <torusGeometry args={[1.68, 0.012, 8, 4]} />
        <meshStandardMaterial color={atmosphere.accent} emissive={atmosphere.aura} emissiveIntensity={1.2} transparent opacity={0.22} />
      </mesh>
      {Array.from({ length: 12 }, (_, index) => {
        const angle = (index / 12) * Math.PI * 2;
        return (
          <mesh key={index} position={[Math.cos(angle) * 2.15, Math.sin(angle) * 2.15, 0]}>
            <sphereGeometry args={[0.035, 10, 10]} />
            <meshBasicMaterial color={atmosphere.accent} transparent opacity={0.6} />
          </mesh>
        );
      })}
    </group>
  );
}

function FloatingMotes({ color, mobile }: { color: string; mobile: boolean }) {
  const points = useRef<THREE.Points>(null);
  const positions = useMemo(() => {
    const count = mobile ? 45 : 90;
    const values = new Float32Array(count * 3);
    let seed = 71;
    const random = () => {
      seed = (seed * 16807) % 2147483647;
      return (seed - 1) / 2147483646;
    };
    for (let index = 0; index < count; index += 1) {
      values[index * 3] = (random() - 0.5) * 9;
      values[index * 3 + 1] = (random() - 0.5) * 6;
      values[index * 3 + 2] = random() * 3 - 0.8;
    }
    return values;
  }, [mobile]);

  useFrame((state, delta) => {
    if (!points.current) return;
    points.current.rotation.y += delta * 0.012;
    points.current.position.y = Math.sin(state.clock.elapsedTime * 0.22) * 0.08;
  });

  return (
    <points ref={points}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <pointsMaterial color={color} size={mobile ? 0.025 : 0.035} transparent opacity={0.5} depthWrite={false} sizeAttenuation />
    </points>
  );
}

function ArcaneBook({ book, rank, index, atmosphere, active, activeIndex, mobile, onActive, onOpen }: {
  book: AuraRecommendation;
  rank: number;
  index: number;
  atmosphere: ArcaneAtmosphere;
  active: boolean;
  activeIndex: number;
  mobile: boolean;
  onActive: (index: number) => void;
  onOpen: (bookId: string) => void;
}) {
  const group = useRef<THREE.Group>(null);
  const texture = useMemo(() => createArcaneBookTexture(book, rank, atmosphere), [atmosphere, book, rank]);
  const placement = DESKTOP_PLACEMENTS[index] ?? DESKTOP_PLACEMENTS[0];
  const mobileOffset = index - activeIndex;
  const basePosition: [number, number, number] = mobile
    ? [mobileOffset * 2.15, 0.05 - Math.abs(mobileOffset) * 0.12, 0.45 - Math.abs(mobileOffset) * 0.58]
    : placement.position;
  const baseRotation: [number, number, number] = mobile
    ? [0, mobileOffset * -0.34, mobileOffset * 0.025]
    : placement.rotation;
  const scale = mobile ? (active ? 1.08 : 0.82) : placement.scale;

  useEffect(() => () => {
    texture.dispose();
    document.body.style.cursor = "";
  }, [texture]);

  useFrame((state, delta) => {
    if (!group.current) return;
    const ease = 1 - Math.exp(-delta * 7);
    const entrance = THREE.MathUtils.clamp((state.clock.elapsedTime - index * 0.11) / 0.75, 0, 1);
    const activeZ = active ? (mobile ? 1.35 : 1.05) : basePosition[2];
    const activeY = basePosition[1] + (active ? 0.16 : 0);
    group.current.position.x = THREE.MathUtils.lerp(group.current.position.x, basePosition[0], ease);
    group.current.position.y = THREE.MathUtils.lerp(group.current.position.y, activeY - (1 - entrance) * 0.7, ease);
    group.current.position.z = THREE.MathUtils.lerp(group.current.position.z, activeZ, ease);
    group.current.rotation.x = THREE.MathUtils.lerp(group.current.rotation.x, baseRotation[0], ease);
    group.current.rotation.y = THREE.MathUtils.lerp(group.current.rotation.y, active ? 0 : baseRotation[1], ease);
    group.current.rotation.z = THREE.MathUtils.lerp(group.current.rotation.z, baseRotation[2], ease);
    const easedScale = scale * (0.72 + entrance * 0.28);
    group.current.scale.setScalar(THREE.MathUtils.lerp(group.current.scale.x, easedScale, ease));
  });

  function stopAndSelect(event: ThreeEvent<PointerEvent>) {
    event.stopPropagation();
    onActive(index);
    document.body.style.cursor = "pointer";
  }

  return (
    <group
      ref={group}
      position={[basePosition[0], basePosition[1] - 0.7, basePosition[2]]}
      rotation={baseRotation}
      scale={0.72}
      onPointerOver={stopAndSelect}
      onPointerOut={() => { document.body.style.cursor = ""; }}
      onClick={(event) => {
        event.stopPropagation();
        onOpen(book.bookId);
      }}
    >
      {!mobile && placement.featured && <BookStand />}
      <mesh castShadow receiveShadow position={[0, 0, 0]}>
        <boxGeometry args={[1.32, 1.96, 0.27]} />
        <meshStandardMaterial color="#d9cfb6" roughness={0.84} />
      </mesh>
      <mesh castShadow position={[0, 0, -0.17]}>
        <boxGeometry args={[1.48, 2.12, 0.09]} />
        <meshPhysicalMaterial color="#1b2927" roughness={0.65} clearcoat={0.18} clearcoatRoughness={0.65} />
      </mesh>
      <mesh castShadow position={[0, 0, 0.18]}>
        <boxGeometry args={[1.48, 2.12, 0.09]} />
        <meshPhysicalMaterial color="#1b2927" roughness={0.62} clearcoat={0.22} clearcoatRoughness={0.6} />
      </mesh>
      <mesh castShadow position={[-0.72, 0, 0]}>
        <boxGeometry args={[0.13, 2.12, 0.38]} />
        <meshStandardMaterial color="#101b1a" roughness={0.72} />
      </mesh>
      <mesh position={[0, 0, 0.231]}>
        <planeGeometry args={[1.38, 2.02]} />
        <meshStandardMaterial map={texture} roughness={0.68} metalness={0.02} />
      </mesh>
      <mesh position={[0.55, -0.92, 0.24]}>
        <circleGeometry args={[0.08, 24]} />
        <meshBasicMaterial color={atmosphere.accent} transparent opacity={active ? 1 : 0.55} />
      </mesh>
    </group>
  );
}

function BookStand() {
  return (
    <group position={[0, -1.16, -0.08]}>
      <mesh castShadow position={[0, 0, 0.12]}>
        <boxGeometry args={[1.72, 0.1, 0.5]} />
        <meshStandardMaterial color="#a47d36" metalness={0.82} roughness={0.25} />
      </mesh>
      <mesh castShadow position={[-0.58, 0.2, -0.08]} rotation={[0, 0, -0.18]}>
        <cylinderGeometry args={[0.035, 0.035, 0.48, 12]} />
        <meshStandardMaterial color="#a47d36" metalness={0.82} roughness={0.25} />
      </mesh>
      <mesh castShadow position={[0.58, 0.2, -0.08]} rotation={[0, 0, 0.18]}>
        <cylinderGeometry args={[0.035, 0.035, 0.48, 12]} />
        <meshStandardMaterial color="#a47d36" metalness={0.82} roughness={0.25} />
      </mesh>
    </group>
  );
}
