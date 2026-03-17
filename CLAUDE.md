# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

A personal wardrobe and outfit tracking web app called **Dressed**. Single HTML file (`index.html`), no dependencies, no server required. Mobile-first design optimized for 430px width.

## Architecture

- Single-file app — all HTML, CSS, and JavaScript in `index.html`
- No build step, no external dependencies
- `localStorage` for all data persistence
- Mobile-first design (430px optimized)
- Google Fonts: Cormorant Garamond + DM Sans

## Features

- **Wardrobe** — Add clothing pieces with photos, category, color, and season tags
- **Outfits** — Build outfits from wardrobe pieces and save them as looks
- **Wear tracking** — Mark items and outfits as worn; tracks total wear count
- **Filter by category** — Tops, Bottoms, Dresses, Shoes, Outerwear, Accessories

## GitHub Workflow

- Repo: **https://github.com/Grayrider2500/outfit-tracker**
- Live URL: **https://grayrider2500.github.io/outfit-tracker/**
- After completing any meaningful changes, commit and push to GitHub
- Use clear, descriptive commit messages

## Conventions

- No external dependencies — everything must work fully offline
- All data stays in the browser, nothing sent to a server
- `font-size: 16px` on all form inputs (prevents iOS auto-zoom)

## Session Memory

After completing any meaningful work session, update the following files:

- **memory.md** — key facts, decisions made, and anything important to remember long-term
- **restart.md** — exactly where you stopped, what you were doing, and what the next step is
- **backlog.md** — everything still left to do, in priority order

If these files don't exist yet, create them. At the start of each new session, read all three files before doing anything else so you resume with full context.
