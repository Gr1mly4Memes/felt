# Felt

A Paper server implementation on Fabric.

![License](https://img.shields.io/github/license/Gr1mly4Memes/felt?style=flat-square)

## Installing

* Download the jar.  
* Launch with command `java -jar felt.jar nogui`. 
  * The `nogui` argument will disable the server control panel.

Read our document for more information.

## Building

This project builds against a pinned PaperMC/Paper commit (see `build-support/paper-sources.json`) via the `upstream/Paper` submodule.

After cloning, initialize and fetch submodules:

```bash
git submodule update --init --recursive
```

If the submodule isn't registered in your clone yet, add it and check out the pinned commit:

```bash
git submodule add https://github.com/PaperMC/Paper.git upstream/Paper
git -C upstream/Paper checkout <commit from build-support/paper-sources.json>
git add upstream/Paper
```

Then build with:

```bash
./gradlew build
./gradlew collect
./gradlew fabricJar
```

## Credits

Felt was originally forked from [Arclight](https://github.com/IzzelAliz/Arclight) by IzzelAliz. Huge thanks to the Arclight project and its contributors — Felt has since diverged significantly and is being developed as its own project.

## License

This project is licensed under [GPL v3](LICENSE).