<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents** 

- [Release workflow](#release-workflow)
  - [Step 1: Release](#step-1-release)
  - [Step 2: Publish](#step-2-publish)
  - [Checklist](#checklist)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Release workflow

The main goal of the build process is to build and publish the docker image to the docker hub repo: [commercetools/commercetools-project-sync](https://hub.docker.com/r/commercetools/commercetools-project-sync/tags)
     
## Step 1: Release

Create a PR for the new release: 
- Increment the release version to the new library version, please follow the [semantic versioning](https://semver.org/) for finding the new version.
- Make sure to update the new version everywhere in the documentation files.
- Ask for review for this PR and then "squash and merge" to master.


------
    
To release the library, you need to [create a new release](https://github.com/commercetools/commercetools-project-sync/releases/new) with Github, 
describe the new release and publish it. 

For example, The release description should show of new features, bug fixes and updates of the dependencies versions. check [previous version tag.](https://github.com/commercetools/commercetools-project-sync/releases/tag/3.10.0)

## Step 2: Publish

- Click the _Publish_ button.

After clicking publish button, Github actions will trigger the CD build. Check [here](https://github.com/commercetools/commercetools-project-sync/actions) for Github actions build status.

### Checklist 

- [Publish](#step-2-publish) completed without an issue.
- The new version image has been published to the docker: [commercetools/commercetools-project-sync](https://hub.docker.com/r/commercetools/commercetools-project-sync/tags)
- Pull the docker image to your local machine and test if the new version works as expected.
