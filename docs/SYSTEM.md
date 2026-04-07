# System & Rolling Engine 🌀

The heart of **SeriaCrate** is its advanced rolling engine and structured reward system. This document explains how they work together.

## 🌀 The Rolling Engine

The Rolling Engine handles the visual animations when a player opens a crate.

### Animation Types
- **Horizontal**: Classic scrolling carousel (left-to-right).
- **Vertical**: Upward scrolling animation.
- **Instant**: No animation, rewards are given immediately.

### Key Features
- **Dynamic Speed**: The animation slows down gradually as it approaches the final reward.
- **Sound Effects**: Customizable sounds for each tick of the scroll.
- **Visual Particles**: Particle effects that trigger when a rare item is rolled.

## 💎 Resin Currency System

**Resin** is the primary currency used for interacting with crates in SeriaCrate.

- **Storage**: Resin can be stored as a virtual currency (linked to Eco/Vault) or as physical items in the inventory.
- **Usage**: Each crate can have a specific Resin cost required for a single or multiple rolls.
- **Commands**: Players can manage their Resin with the `/resin` command.

## 📦 Reward Tiers (Classes)

Rewards are organized into **Tiers** (sometimes called Classes) to manage drop probabilities.

### Tier Structure
Each tier has its own weight and visual style:
- **Common**: High probability, simple effects.
- **Rare**: Medium probability, distinctive particles.
- **Legendary**: Low probability, epic announcement and effects.

### How it Works
1. When a crate is opened, the system first rolls to determine the **Tier**.
2. Once a Tier is selected, it rolls again to find a specific **Reward** within that Tier.

---
*Return to [README](../README.md)*
