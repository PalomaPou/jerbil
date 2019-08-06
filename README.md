# jerbil
A *very* simple fast file-based CMS: markdown format text files + html templates.

Status: alpha   
We use this tool internally and it is 100% ready. We are working on nice packaging for others to use.

## Basic use

1. Clone the example jerbil site (TODO: link here) to get started quickly.
2. Write content in the pages directory. You can mix markdown and html.
3. Run Jerbil

## Variables

You can define variables at the start of a page file, using the simple format:

  key: value

Then use them within templates or page contents via `$key`.

## Templates and Imports: `<section>`

You can pull content from other files into a page and use templates. E.g.

* `<section src='myfooter' />` Load myfooter.html or myfooter.md and insert it here.
* `<section src='myarticletemplate.html'>Blah blah</section>` Use myarticletemplate.html as a template with the contents "Blah blah".	


## Installation

Depends on: the libraries in the [open-code](https://github.com/sodash/open-code) repo.

## Credits

Built using Java by the [Good-Loop](http://good-loop.com/?utm_source=winterstein&utm_medium=code&utm_campaign=jerbil) team.
