#!/usr/bin/ruby -p
while ($_.index("\t") != nil) do
  $_.sub!(/\t/) { " " * (8 - ($`.length % 8)) }
end
