# PR Auto Labeler Action

A JavaScript GitHub Action that automatically labels pull requests based on the files changed.

## Features

- Automatically detects changed files in pull requests
- Applies labels based on configurable file patterns
- Supports glob patterns for flexible file matching
- Avoids duplicate labels
- Provides detailed logging and summaries

## Usage

### Basic Usage

```yaml
- name: Auto label PR
  uses: ./.github/actions/javascript-label-action
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

### Custom Label Configuration

```yaml
- name: Auto label PR with custom config
  uses: ./.github/actions/javascript-label-action
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    label-config: |
      {
        "src/main/java/**": ["java", "backend"],
        "src/test/**": ["tests"],
        ".github/workflows/**": ["ci/cd", "automation"],
        "docs/**": ["documentation"],
        "*.md": ["documentation"],
        "pom.xml": ["dependencies", "build"],
        "Dockerfile": ["docker", "infrastructure"]
      }
```

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `github-token` | GitHub token for API access | Yes | N/A |
| `label-config` | JSON configuration mapping file patterns to labels | No | Default configuration |

### Default Label Configuration

```json
{
  "src/main/java/**": ["java", "backend"],
  "src/test/**": ["tests"],
  ".github/workflows/**": ["ci/cd"],
  "docs/**": ["documentation"],
  "*.md": ["documentation"],
  "pom.xml": ["dependencies"],
  "Dockerfile": ["docker"]
}
```

## Outputs

| Output | Description |
|--------|-------------|
| `labels-added` | Comma-separated list of labels that were added to the PR |

## How It Works

1. The action retrieves all files changed in the pull request
2. Each changed file is matched against the configured patterns
3. Labels associated with matching patterns are collected
4. Only new labels (not already on the PR) are applied
5. A summary is generated showing which labels were added

## Pattern Matching

The action uses the [minimatch](https://github.com/isaacs/minimatch) library for glob pattern matching. Supported patterns include:

- `**` - Match any number of directories
- `*` - Match any characters except `/`
- `?` - Match a single character
- `[abc]` - Match one of the characters
- `{a,b}` - Match one of the patterns

### Pattern Examples

```json
{
  "src/**/*.java": ["java"],
  "src/main/**": ["backend"],
  "src/test/**": ["tests"],
  "*.{yml,yaml}": ["configuration"],
  "docs/*.md": ["documentation"]
}
```

## Development

### Building the Action

```bash
cd .github/actions/javascript-label-action
npm install
npm run build
```

This will compile the action and its dependencies into `dist/index.js`.

### Testing

```bash
npm test
```

## Using in Separate Repository

This action can be moved to its own repository for better reusability. When doing so:

1. Create a new repository for the action
2. Copy the action files to the repository root
3. Build and commit the `dist/` folder
4. Tag releases for versioning (e.g., `v1.0.0`)
5. Reference the action in workflows:

```yaml
- uses: your-org/javascript-label-action@v1
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

## Accessing Private Actions

To use this action from a private repository, you can use a GitHub App token:

```yaml
- name: Generate GitHub App Token
  id: generate-token
  uses: actions/create-github-app-token@v1
  with:
    app-id: ${{ secrets.APP_ID }}
    private-key: ${{ secrets.APP_PRIVATE_KEY }}

- name: Checkout private action
  uses: actions/checkout@v4
  with:
    repository: your-org/javascript-label-action
    ref: v1
    token: ${{ steps.generate-token.outputs.token }}
    path: .github/actions/label-action

- name: Use private action
  uses: ./.github/actions/label-action
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

## License

MIT
