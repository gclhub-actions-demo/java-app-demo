const core = require('@actions/core');
const github = require('@actions/github');
const { minimatch } = require('minimatch');

/**
 * Main function to execute the action
 */
async function run() {
  try {
    // Get inputs
    const token = core.getInput('github-token', { required: true });
    const labelConfigStr = core.getInput('label-config', { required: false });
    
    // Parse label configuration
    const labelConfig = JSON.parse(labelConfigStr);
    
    // Get GitHub context
    const context = github.context;
    
    // Ensure this is a pull request event
    if (!context.payload.pull_request) {
      core.warning('This action only works on pull_request events');
      return;
    }
    
    const prNumber = context.payload.pull_request.number;
    const octokit = github.getOctokit(token);
    
    // Get list of changed files in the PR
    const { data: files } = await octokit.rest.pulls.listFiles({
      owner: context.repo.owner,
      repo: context.repo.repo,
      pull_number: prNumber
    });
    
    core.info(`Found ${files.length} changed files in PR #${prNumber}`);
    
    // Determine which labels should be applied
    const labelsToAdd = new Set();
    
    for (const file of files) {
      const filename = file.filename;
      core.debug(`Processing file: ${filename}`);
      
      // Check each pattern in the configuration
      for (const [pattern, labels] of Object.entries(labelConfig)) {
        if (minimatch(filename, pattern, { matchBase: true })) {
          core.debug(`File ${filename} matches pattern ${pattern}`);
          labels.forEach(label => labelsToAdd.add(label));
        }
      }
    }
    
    if (labelsToAdd.size === 0) {
      core.info('No labels to add based on changed files');
      return;
    }
    
    // Get existing labels on the PR
    const { data: currentLabels } = await octokit.rest.issues.listLabelsOnIssue({
      owner: context.repo.owner,
      repo: context.repo.repo,
      issue_number: prNumber
    });
    
    const existingLabels = new Set(currentLabels.map(label => label.name));
    
    // Filter out labels that are already applied
    const newLabels = Array.from(labelsToAdd).filter(
      label => !existingLabels.has(label)
    );
    
    if (newLabels.length === 0) {
      core.info('All applicable labels are already present');
      return;
    }
    
    // Add the labels
    core.info(`Adding labels: ${newLabels.join(', ')}`);
    
    await octokit.rest.issues.addLabels({
      owner: context.repo.owner,
      repo: context.repo.repo,
      issue_number: prNumber,
      labels: newLabels
    });
    
    // Set output
    core.setOutput('labels-added', newLabels.join(','));
    
    // Add summary
    core.summary
      .addHeading('PR Auto Labeler Results')
      .addList(newLabels.map(label => `Added label: \`${label}\``))
      .write();
    
    core.info(`Successfully added ${newLabels.length} labels to PR #${prNumber}`);
    
  } catch (error) {
    core.setFailed(`Action failed with error: ${error.message}`);
  }
}

run();
