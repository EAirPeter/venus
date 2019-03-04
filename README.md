# venus164

__venus__ is a RISC-V instruction set simulator built for education. This repository is a fork of [kvakil/venus](https://github.com/kvakil/venus). The fork contains some additional minor features that were found to be useful when implementing a compiler for the [ChocoPy](https://chocopy.org) language. This fork is therefore called Venus164 (named after the course CS164 at UC Berkeley). The additions in this fork are all standard assembler features that are supported by the [official RISC-V GNU-based toolchain](https://github.com/riscv/riscv-tools).

## Using venus164

venus164 is [available online](https://chocopy.github.io/venus).

## Features (from the [original](https://github.com/kvakil/venus))
* RV32IM
* Single-step debugging with undo feature
* Breakpoint debugging
* View machine code and original instructions side-by-side
* Several `ecall`s: including `print` and `sbrk`
* Memory visualization

### Features Added for Venus164

* `.word <label>` directive - [Relocatable data words](https://github.com/kvakil/venus/pull/20)
* `.align <n>` directive - [Align data words to powers of 2](https://github.com/kvakil/venus/pull/21)
* `.string` directive - [alias for `.asciiz`](https://github.com/kvakil/venus/pull/22)

## Building

Build the backend:

    $ ./gradlew build

Build the frontend+backend bundle:

    $ grunt dist

## Resources

#### [User Guide](https://github.com/kvakil/venus/wiki)

#### [MIT License](https://github.com/chocopy/venus/blob/master/LICENSE)
