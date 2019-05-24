# venus - JVM branch

This branch should always be ahead of `master`. It contains some changes from `master` necessary to compile Venus to the JVM.

## Create venus JAR

Run `gradle clean jar` from root.

## Keep it synced

Remember 1: If there is a need to add any new feature to Venus in general (e.g. its assembler or simulator), please make the changes in `master` and then merge them in to `jvm`. Do not make the changes in `jvm` directly.

Remember 2: If there are any changes to `master`, please run `git merge master` from this branch.

## New ecalls

The following ecalls handle input from standard input.

         8:   read_string
        18:   fill_line_buffer

The `fill_line_buffer` call takes no arguments.  It reads a line from
the standard input, not including the line terminator, into an
internal line buffer, replacing any previous contents and returning
the number of bytes read.

The `read_string` call takes an address, A, in a1 and a length, n, in
a2.  It reads up to n bytes from the internal line buffer into memory
starting at A, returning (in a0) the number of bytes actually read.
