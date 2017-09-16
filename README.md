# jerbil
A *very* simple fast file-based CMS: markdown format text files + html templates.

Status: NOT READY YET! 
We use this tool internally. We are working on packaging it for others to use.

## Basic use

1. Clone the example jerbil site (TODO: link here) to get started quickly.
2. Write content in the pages directory. You can mix markdown and html.
3. Run Jerbil

## Variables

You can define variables at the start of a page file, using the simple format:

  key: value

Then use them within templates or page contents via `$key`.

## Installation

Depends on: the libraries in the [open-code](https://github.com/sodash/open-code) repo.

## Credits

Built using Java by the [Good-Loop](http://good-loop.com/?utm_source=winterstein&utm_medium=code&utm_campaign=jerbil) team.
