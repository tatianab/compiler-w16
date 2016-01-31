@echo off

echo Compiling...
javac Parser.java

echo Testing...
echo.
echo ---------------------------------
echo Running big...
java Parser my-tests/big.txt
echo ---------------------------------

echo.
echo ---------------------------------
echo Running cell...
java Parser my-tests/cell.txt
echo ---------------------------------

echo.
echo ---------------------------------
echo Running factorial...
java Parser my-tests/factorial.txt
echo ---------------------------------

echo.
echo ---------------------------------
echo Running parser-test...
java Parser my-tests/parser-test.txt
echo ---------------------------------

set x=1
:loop
   REM : Pad value.
   set padded=00%x%
   set padded=%padded:~-2%

   REM: Run Parser.
   echo.
   echo ---------------------------------
   echo Running test0%padded%...
   java Parser my-tests/test0%padded%.txt
   echo ---------------------------------
   

   REM: Increment value.
   set /A x+=1
if %x% leq 31 goto loop