package com.focuscrew.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// FocusCrew — Old Money Quiet Luxury: Daylight Editorial Design Tokens
// 100% Light Theme • Soft Warm Espresso Ink • Tactile Cashmere & Brass
// =========================================================================

// Canvas & Surfaces
val CashmereCream = Color(0xFFFBF9F5)          // Canvas background (warm alabaster)
val DaylightPaper = Color(0xFFFFFFFF)          // Elevated crisp cards
val WarmLinen = Color(0xFFF2EEE5)              // Surface container (tactile warm linen)
val WarmLinenHigh = Color(0xFFECE7DC)          // Surface container high
val WarmLinenDim = Color(0xFFF0ECE3)           // Dim surface
val TerracottaTint = Color(0xFFFFF8F6)         // Blushed linen card for reflection/friction

// Soft Ink Typography (NEVER STARK BLACK #000000)
val SoftEspressoInk = Color(0xFF202621)        // Primary text, numbers & headers
val MutedCharcoalOlive = Color(0xFF5E665F)     // Secondary text, captions, subtitles

// Horological Accents
val AntiqueHoneyBrass = Color(0xFFA37A3E)       // Primary buttons, chronometer rings, active icons
val AntiqueHoneyBrassHover = Color(0xFF8A642E)  // Pressed / hover state
val AntiqueHoneyBrassContainer = Color(0xFFF5EAD8)
val OnAntiqueHoneyBrassContainer = Color(0xFF453012)

// Restorative Purity & Verification
val BritishRacingSage = Color(0xFF284B39)       // Calm focus, verified badges, shields
val BritishRacingSageContainer = Color(0xFFE1EDE5)
val OnBritishRacingSageContainer = Color(0xFF132C20)
val SagePillBg = Color(0xFFEAF1EC)              // Soft sage pill background

// Friction & Reality Check (Gentle Terracotta, Not Punitive Neon)
val TerracottaClay = Color(0xFF8F3B3B)          // 30s pause, trance-breaker alerts
val TerracottaContainer = Color(0xFFFCE8E8)
val OnTerracottaContainer = Color(0xFF421414)

// Hairline Borders & Dividers
val WhisperBorder = Color(0x29A37A3E)           // 1px hairline antique brass border (16% alpha)
val OutlineColor = Color(0xFFD8D1C3)            // Sand outline
val OutlineVariantColor = Color(0xFFE7E2D8)     // Soft separator

// Backward-Compatibility Aliases
val LightSurface = CashmereCream
val LightSurfaceVariant = WarmLinen
val LightOnSurface = SoftEspressoInk
val LightOnSurfaceVariant = MutedCharcoalOlive
val LightPrimary = AntiqueHoneyBrass
val LightOnPrimary = Color(0xFFFFFFFF)
val LightCalmAccent = BritishRacingSage
val LightFrictionAlert = TerracottaClay

val DarkSurface = CashmereCream
val DarkSurfaceVariant = WarmLinen
val DarkOnSurface = SoftEspressoInk
val DarkOnSurfaceVariant = MutedCharcoalOlive
val DarkPrimary = AntiqueHoneyBrass
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkCalmAccent = BritishRacingSage
val DarkFrictionAlert = TerracottaClay
