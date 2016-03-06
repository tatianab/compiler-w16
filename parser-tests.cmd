@echo off

echo Testing...                              > result.out 2>&1                              
echo.                                       >> result.out 2>&1

set x=1
:loop
   REM : Pad value.
   set padded=00%x%
   set padded=%padded:~-2%

   REM: Run compiler.
   echo.                                    >> result.out 2>&1 
   echo ---------------------------------   >> result.out 2>&1   
   echo Running test0%padded%...            >> result.out 2>&1
   java Compiler my-tests/test0%padded%.txt -all >> result.out 2>&1
   echo ---------------------------------   >> result.out 2>&1
   

   REM: Increment value.
   set /A x+=1
if %x% leq 31 goto loop