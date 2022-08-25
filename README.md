# plugin-test


## Dataset 

To compute fully annotated stacktrace by `intellij_fixed` dataset `com.github.drazumova.plugintest.dataset.Main.main`
is used. Command line args: 
1. Directory with `issue_report_ids.csv` file
2. Directory with reports files. Only `report_id.json` files used
3. Path to intellij repository
4. Output directory path

Resulting format for one report: 

`report_id/`\
  `- report_id.json` information about start commit, fix commit and stacktrace \
  `- files/`\
  `-- filename.java` full text of file mentioned in the stacktrace line\
  `-- filename.java.json` vcs annotation for every line



To get features from annotated reports run `com.github.drazumova.plugintest.dataset.Features.main` 
with following argument:
1. Path to dataset
2. Output file

Generated header:\
``reportId,lineNumber,isFirstLine,isLastModified,editable,label``\
Rows example:\
``1501544,0,1,0,1,1``\
``1501544,1,0,0,1,0``\
``1501544,2,0,0,1,0``


## Plugin model 

For stacktrace realtime highlighting `com.github.drazumova.plugintest.models.LinearModel` requires configuration 
stored in `resources/model_params.json`. 

Default value: ``{
"weights": [1.0, 1.0, 0.0]
}``



















![Build](https://github.com/drazumova/plugin-test/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "plugin-test"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/drazumova/plugin-test/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
