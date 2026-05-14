---
name: code-review
description: Review code for bugs, security issues, and best practices
allowed-tools: Read Bash
---

# Code Review Instructions

When reviewing code, follow this structured approach:

## 1. Security Check
- Look for SQL injection, XSS, command injection
- Check for hardcoded secrets or credentials
- Verify input validation at boundaries

## 2. Logic & Correctness
- Trace edge cases (null, empty, boundary values)
- Verify loop termination conditions
- Check error handling completeness

## 3. Style & Maintainability
- Consistent naming conventions
- No dead code or unreachable branches
- Methods under 30 lines where possible

## Output Format
Report findings as:
- **CRITICAL**: Security or data-loss bugs
- **WARNING**: Logic issues or potential bugs
- **INFO**: Style or maintainability suggestions
