---
name: Heritage Industrial
colors:
  surface: '#FCF9F8'
  surface-dim: '#ddd9d8'
  surface-bright: '#fdf8f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f7f2f2'
  surface-container: '#f1edec'
  surface-container-high: '#ece7e7'
  surface-container-highest: '#e6e1e1'
  on-surface: '#1c1b1b'
  on-surface-variant: '#5c403e'
  inverse-surface: '#313030'
  inverse-on-surface: '#f4f0ef'
  outline: '#906f6d'
  outline-variant: '#e5bdba'
  surface-tint: '#bf0522'
  primary: '#9f001a'
  on-primary: '#ffffff'
  primary-container: '#c91428'
  on-primary-container: '#ffdcd9'
  inverse-primary: '#ffb3af'
  secondary: '#5f5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e2dfdf'
  on-secondary-container: '#636262'
  tertiary: '#4e4d4d'
  on-tertiary: '#ffffff'
  tertiary-container: '#666565'
  on-tertiary-container: '#e6e3e2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad7'
  primary-fixed-dim: '#ffb3af'
  on-primary-fixed: '#410005'
  on-primary-fixed-variant: '#930017'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c6'
  on-secondary-fixed: '#1c1b1c'
  on-secondary-fixed-variant: '#474647'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1c1b1b'
  on-tertiary-fixed-variant: '#474746'
  background: '#fdf8f8'
  on-background: '#1c1b1b'
  surface-variant: '#e6e1e1'
  denim-texture-opacity: rgba(28, 27, 27, 0.1)
  success-emerald: '#059669'
  warning-amber: '#D97706'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '700'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  label-bold:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '700'
    lineHeight: '1.2'
  label-caps:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  display-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  sidebar-width: 16rem
  header-height: 5rem
  container-max: 80rem
  gutter: 2.5rem
  card-padding: 2rem
  stack-sm: 1rem
  stack-md: 1.5rem
  stack-lg: 4rem
---

## Brand & Style
The brand identity merges the rugged, authentic heritage of industrial denim manufacturing with high-precision AI technology. The aesthetic is "Modern Industrial"—clean and professional, yet rooted in physical textures. It evokes trust through structural clarity and innovation through sophisticated data presentation.

The UI utilizes a "Glass-Industrial" hybrid approach: solid, high-contrast structural elements (like the deep charcoal sidebar) contrasted with airy, semi-transparent surfaces (glassmorphism) in the main content area. This creates a sense of depth and focus, positioning the tool as a premium, authoritative portal for staff management.

## Colors
The palette is dominated by "Levi’s Red" (#C91428) used as a high-impact action color against a sophisticated "Off-White" surface (#FCF9F8). 

- **Primary:** The iconic red is used sparingly for primary actions, progress indicators, and critical brand touchpoints.
- **Surface Hierarchy:** Uses a tiered system from "Lowest" (pure white for cards) to "Dim" (#DCD9D9) for subtle backgrounds.
- **Inverse Surface:** A deep, near-black charcoal (#313030) is reserved for the global navigation, providing a strong structural anchor.
- **Accents:** High-vibrancy emerald and amber are used strictly for status indicators and AI "score" metrics to ensure immediate legibility.

## Typography
The system uses a pairing of **Space Grotesk** for structural/technical impact and **Manrope** for functional clarity.

- **Space Grotesk** is used for all "Display" and "Headline" roles. Its geometric, slightly technical character reinforces the AI-driven nature of the platform.
- **Manrope** handles all reading and interface tasks. Its balanced proportions ensure high legibility in data-dense tables and configuration panels.
- **Tracking:** Negative letter spacing is applied to large display headers to maintain a compact, premium feel. Increased tracking is applied to uppercase labels to improve scanability at small sizes.

## Layout & Spacing
The layout follows a **Fixed Sidebar + Fluid Main Content** model. 

- **Sidebar:** A constant 256px (16rem) anchor on the left.
- **Main Content:** Centered within a 1280px (80rem) max-width container to maintain line-length readability on ultra-wide displays.
- **Bento Grid Logic:** The dashboard utilizes a Bento-style layout for the "Motor Parameters" and "Data Area," where the left column occupies 1/3 and the right 2/3 of the horizontal space.
- **Responsive Behavior:** On mobile, the sidebar collapses into a bottom-bar or hamburger menu, and the 2/3 + 1/3 grid stacks vertically into a single column with reduced horizontal padding (1rem).

## Elevation & Depth
Depth is achieved through a combination of **Surface Toning** and **Glassmorphism**:

- **Level 0 (Background):** `surface` (#FCF9F8).
- **Level 1 (Cards):** `surface-container-lowest` (#FFFFFF) with an extremely soft ambient shadow `rgba(28, 27, 27, 0.03)` and a subtle `surface-variant` border.
- **Level 2 (Overlays/Headers):** The top navigation uses a "Glass" effect: `rgba(252, 249, 248, 0.8)` background with a 20px backdrop blur.
- **Sidebar Depth:** The sidebar uses a "Projected Shadow" `rgba(28, 27, 27, 0.06)` cast to the right, visually separating the global navigation from the workspace.

## Shapes
The shape language is "Soft-Industrial." While the brand is rugged, the digital interface uses controlled roundedness to feel modern and accessible.

- **Base Radius:** 0.25rem (4px) for inputs and small buttons.
- **Large Radius:** 0.75rem (12px) for cards and section containers.
- **Interactive Radius:** 0.5rem (8px) for primary navigation items and CTA buttons.
- **Utility:** Standard full pill shapes are used exclusively for status chips and toggle switches.

## Components
- **Buttons:** 
  - *Primary:* Red background, white bold text, shadow on hover.
  - *Secondary:* Inverse-surface (charcoal) background for high-contrast alternative actions.
  - *Outline:* Thin border using `outline` color, transitioning to brand red on hover.
- **Inputs:** Underlined-style selects and text fields using `surface-container-low` backgrounds and 2px bottom borders for a technical, "form-fill" feel.
- **Cards:** White base, subtle border, and internal padding of 2rem. Used to group "Bento" sections.
- **Toggles:** Custom rounded switch using `primary-container` for the "on" state and `gray-300` for "off."
- **Data Tables:** High-density, minimalist design with `divide-y` borders and hover states using `surface-container-low/50`.
- **Status Chips:** Small, uppercase labels with 10% opacity backgrounds of the status color (e.g., Red 10% for "Recommended").