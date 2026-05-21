#!/usr/bin/env node
// gen-tokens.mjs
// Reads ../shared/design-tokens.json and emits:
//   ../shared/generated/Tokens.swift
//   ../shared/generated/Tokens.kt
//
// Override outputs via env: TOKENS_IOS_OUT, TOKENS_ANDROID_OUT.
// Once Xcode/Gradle projects exist, point those at the project's source tree
// (e.g. ios/TravelTranslator/DesignSystem/Tokens.swift,
//        android/app/src/main/java/com/divami/traveltranslator/designsystem/Tokens.kt)
// and wire this script into the build (Xcode Run Script Phase / Gradle task).
//
// NOT codegen'd: languages, context modes, playback speeds.
// Those are app config, not visual tokens — hand-code enums and load translations
// from JSON resources at runtime.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..');
const tokensJsonPath = join(root, 'shared', 'design-tokens.json');
const iosOut     = process.env.TOKENS_IOS_OUT     || join(root, 'shared', 'generated', 'Tokens.swift');
const androidOut = process.env.TOKENS_ANDROID_OUT || join(root, 'shared', 'generated', 'Tokens.kt');

const tokens = JSON.parse(readFileSync(tokensJsonPath, 'utf8'));

// --- helpers ---
const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);
const onlyPublic = (obj) =>
  Object.fromEntries(Object.entries(obj).filter(([k]) => !k.startsWith('_')));

// "2xl" is not a valid identifier in Swift or Kotlin — alias it.
const sizeKeyAlias = (k) => (k === '2xl' ? 'xxl' : k);

function parseColor(value) {
  if (value === 'transparent') return { argb: 0x00000000, transparent: true };
  if (value.startsWith('#')) {
    const v = value.slice(1);
    const expanded = v.length === 6 ? 'FF' + v : v;
    return { argb: parseInt(expanded, 16) >>> 0, transparent: false };
  }
  if (value.startsWith('rgba')) {
    const [r, g, b, a] = value.match(/[\d.]+/g).map(Number);
    const aa = Math.round(a * 255);
    return { argb: ((aa << 24) | (r << 16) | (g << 8) | b) >>> 0, transparent: false };
  }
  throw new Error(`Unknown color format: ${value}`);
}
const argbHex = (n) => `0x${n.toString(16).toUpperCase().padStart(8, '0')}`;
const swiftColor = (value) => {
  const c = parseColor(value);
  return c.transparent ? 'Color.clear' : `Color(argb: ${argbHex(c.argb)})`;
};
const kotlinColor = (value) => {
  const c = parseColor(value);
  return c.transparent ? 'Color.Transparent' : `Color(${argbHex(c.argb)})`;
};

// --- Swift ---
function genSwift() {
  const L = [];
  L.push('// AUTO-GENERATED. Do not edit. Source: shared/design-tokens.json');
  L.push('// Run `node tools/gen-tokens.mjs` to regenerate.');
  L.push('');
  L.push('import SwiftUI');
  L.push('');
  L.push('public enum DesignTokens {');

  L.push('    public enum Brand {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.brand))) {
    L.push(`        public static let ${k} = ${swiftColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    public enum Semantic {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.semantic))) {
    L.push(`        public static let ${k} = ${swiftColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    public enum Neutral {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.neutral))) {
    L.push(`        public static let ${k} = ${swiftColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    public struct CategoryColors: Sendable {');
  L.push('        public let bg: Color');
  L.push('        public let fg: Color');
  L.push('    }');
  L.push('    public enum Category {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.category))) {
    L.push(`        public static let ${k} = CategoryColors(bg: ${swiftColor(v.bg)}, fg: ${swiftColor(v.fg)})`);
  }
  L.push('    }', '');

  L.push('    public enum Dark {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.darkMode))) {
    L.push(`        public static let ${k} = ${swiftColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    public enum Radius {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.radius))) {
    L.push(`        public static let ${k}: CGFloat = ${v.value}`);
  }
  L.push('    }', '');

  L.push('    public enum Spacing {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.spacing))) {
    L.push(`        public static let s${k}: CGFloat = ${v.value}`);
  }
  L.push('    }', '');

  L.push('    public enum Typography {');
  L.push(`        public static let weightNormal: Font.Weight = .regular`);
  L.push(`        public static let weightMedium: Font.Weight = .medium`);
  L.push(`        public static let rootFontSize: CGFloat = ${tokens.typography.rootFontSize.value}`);
  L.push('        public enum Size {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.typography.fontSize))) {
    L.push(`            public static let ${sizeKeyAlias(k)}: CGFloat = ${v.value}`);
  }
  L.push('        }');
  L.push(`        public static let lineHeight: CGFloat = ${tokens.typography.lineHeight.default}`);
  L.push('    }', '');

  L.push('    public enum Motion {');
  L.push('        public enum Easing {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.easing))) {
    const [a, b, c, d] = v.value.match(/[\d.]+/g).map(Number);
    L.push(`            public static let ${k}: (Double, Double, Double, Double) = (${a}, ${b}, ${c}, ${d})`);
  }
  L.push('        }');
  L.push('        public enum Duration {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.duration))) {
    L.push(`            public static let ${k}: TimeInterval = ${v.value / 1000}`);
  }
  L.push('        }');
  L.push('        public enum PressScale {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.pressScale))) {
    L.push(`            public static let ${k}: CGFloat = ${v.value}`);
  }
  L.push('        }');
  L.push('    }', '');

  L.push('    public enum TouchTarget {');
  L.push(`        public static let min: CGFloat = ${tokens.touchTarget.minIos.value}`);
  L.push('    }');
  L.push('}', '');

  L.push('public extension Color {');
  L.push('    init(argb: UInt32) {');
  L.push('        let a = Double((argb >> 24) & 0xFF) / 255');
  L.push('        let r = Double((argb >> 16) & 0xFF) / 255');
  L.push('        let g = Double((argb >>  8) & 0xFF) / 255');
  L.push('        let b = Double( argb        & 0xFF) / 255');
  L.push('        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)');
  L.push('    }');
  L.push('}');

  return L.join('\n') + '\n';
}

// --- Kotlin ---
function genKotlin() {
  const L = [];
  L.push('// AUTO-GENERATED. Do not edit. Source: shared/design-tokens.json');
  L.push('// Run `node tools/gen-tokens.mjs` to regenerate.');
  L.push('package com.divami.traveltranslator.designsystem');
  L.push('');
  L.push('import androidx.compose.animation.core.CubicBezierEasing');
  L.push('import androidx.compose.ui.graphics.Color');
  L.push('import androidx.compose.ui.unit.dp');
  L.push('import androidx.compose.ui.unit.sp');
  L.push('');
  L.push('object DesignTokens {');

  L.push('    object Brand {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.brand))) {
    L.push(`        val ${cap(k)} = ${kotlinColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    object Semantic {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.semantic))) {
    L.push(`        val ${cap(k)} = ${kotlinColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    object Neutral {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.neutral))) {
    L.push(`        val ${cap(k)} = ${kotlinColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    data class CategoryColors(val bg: Color, val fg: Color)');
  L.push('    object Category {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.category))) {
    L.push(`        val ${cap(k)} = CategoryColors(${kotlinColor(v.bg)}, ${kotlinColor(v.fg)})`);
  }
  L.push('    }', '');

  L.push('    object Dark {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.color.darkMode))) {
    L.push(`        val ${cap(k)} = ${kotlinColor(v.value)}`);
  }
  L.push('    }', '');

  L.push('    object Radius {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.radius))) {
    L.push(`        val ${cap(k)} = ${v.value}.dp`);
  }
  L.push('    }', '');

  L.push('    object Spacing {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.spacing))) {
    L.push(`        val S${k} = ${v.value}.dp`);
  }
  L.push('    }', '');

  L.push('    object Typography {');
  L.push(`        const val WeightNormal = ${tokens.typography.fontWeightNormal}`);
  L.push(`        const val WeightMedium = ${tokens.typography.fontWeightMedium}`);
  L.push(`        val RootFontSize = ${tokens.typography.rootFontSize.value}.sp`);
  L.push('        object Size {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.typography.fontSize))) {
    L.push(`            val ${cap(sizeKeyAlias(k))} = ${v.value}.sp`);
  }
  L.push('        }');
  L.push(`        const val LineHeight = ${tokens.typography.lineHeight.default}f`);
  L.push('    }', '');

  L.push('    object Motion {');
  L.push('        object Easing {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.easing))) {
    const [a, b, c, d] = v.value.match(/[\d.]+/g).map(Number);
    L.push(`            val ${cap(k)} = CubicBezierEasing(${a}f, ${b}f, ${c}f, ${d}f)`);
  }
  L.push('        }');
  L.push('        object Duration {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.duration))) {
    L.push(`            const val ${cap(k)}Ms = ${v.value}`);
  }
  L.push('        }');
  L.push('        object PressScale {');
  for (const [k, v] of Object.entries(onlyPublic(tokens.motion.pressScale))) {
    L.push(`            const val ${cap(k)} = ${v.value}f`);
  }
  L.push('        }');
  L.push('    }', '');

  L.push('    object TouchTarget {');
  L.push(`        val Min = ${tokens.touchTarget.minAndroid.value}.dp`);
  L.push('    }');
  L.push('}');

  return L.join('\n') + '\n';
}

// --- write ---
mkdirSync(dirname(iosOut),     { recursive: true });
mkdirSync(dirname(androidOut), { recursive: true });
writeFileSync(iosOut,     genSwift());
writeFileSync(androidOut, genKotlin());
console.log(`✓ ${iosOut}`);
console.log(`✓ ${androidOut}`);
