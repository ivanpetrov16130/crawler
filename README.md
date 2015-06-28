# crawler

Just primitive web-crawler, which helps you to get all the "usable" urls from some pages.

## Installation

WTF is installation? I'm noob, download it and compile!

## Usage

Lol, run it in lein and open your found urls recorded in file("out__urls.txt").
And add your urls to file "urls.txt" for crawling, before you start.

    $ cd MyWayToClojureProjects/crawler
    $ lein run depth

## Options

--depth

How many times crawler should process bunches of urls? That's it - a number of waves!

## Examples

1)Add to file "urls.txt" url:

    http://www.example.com

  and save file. Use only full url, like in example above.

2)
    $ cd  MyWayToClojureProjects/crawler

3)
    $ lein run 3

4) Open file "out__urls"

5) ????

6) PROFIT!!!!

### Bugs

 ¯\\_(ツ)_/¯

### That You Think

Law doesn't work, Authorities are illegal, Society is rotting.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
