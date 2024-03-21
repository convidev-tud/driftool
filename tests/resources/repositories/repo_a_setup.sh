#!/bin/bash

mkdir -p tests/resources/repositories/repo_a

cd tests/resources/repositories/repo_a

git init

git config user.name "drifool"
git config user.email "git@driftool.io"

git add .
git commit -m "Initial commit"

echo '
a
b
c
d
e
' > letters.txt

echo '
1
2
3
4
5
' > numbers.txt

git add .
git commit -m "Add letters and numbers"

# Branch additive_feature contains a new feature without conflicts
git checkout -b additive_feature

echo '
+
-
#
:
;
' > symbols.txt

git add .
git commit -m "Add symbols"

git checkout main

git checkout -b conflicting_feature_a

echo '
a
kkk
xxx
yyy
e
' > letters.txt

git add .
git commit -m "Change letters"

echo '
1
200
3
4
500
' > numbers.txt

git add .
git commit -m "Change numbers"

git checkout main

git checkout -b conflicting_feature_b

echo '
a
ooo
www
ttt
e
' > letters.txt

echo '
100
300
3
4
5
' > numbers.txt

git add .
git commit -m "Change letters and numbers"

git checkout main

# merge conflict b into conflict a = 7 + 7 = 14

#a
#b
#<<<<<<< HEAD
#xxx
#yyy
#=======
#www
#ttt
#>>>>>>> conflicting_feature_b
#e

#<<<<<<< HEAD
#1
#200
#=======
#100
#300
#>>>>>>> conflicting_feature_b
#3
#4
#500
