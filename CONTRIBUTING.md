# Contributing to Java App Demo

Thank you for considering contributing to this project! This guide will help you get started.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and collaborative environment.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in Issues
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, etc.)

### Suggesting Enhancements

1. Check existing issues for similar suggestions
2. Create a new issue describing:
   - The enhancement you'd like to see
   - Why it would be useful
   - Possible implementation approach

### Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes following our coding standards
4. Write or update tests as needed
5. Ensure all tests pass
6. Update documentation if needed
7. Submit a pull request

## Development Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/java-app-demo.git
cd java-app-demo

# Add upstream remote
git remote add upstream https://github.com/original-org/java-app-demo.git

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: add amazing feature"

# Push to your fork
git push origin feature/your-feature-name
```

## Coding Standards

- Follow Google Java Style Guide
- Write meaningful commit messages
- Keep methods under 150 lines
- Maintain test coverage above 70%
- Document public APIs with Javadoc

## Testing

All contributions must include appropriate tests:

```bash
# Run tests
mvn test

# Check coverage
mvn verify
```

## Commit Message Convention

Follow conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test additions or changes
- `refactor:` Code refactoring
- `chore:` Maintenance tasks

Example:
```
feat: add customer export functionality

- Add CSV export endpoint
- Include unit tests
- Update API documentation
```

## Review Process

1. Automated checks must pass
2. At least one maintainer review required
3. No unresolved conversations
4. Branch must be up-to-date with main

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
