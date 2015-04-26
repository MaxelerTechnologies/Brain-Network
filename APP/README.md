Information to compile
======================
Ensure the environment variables below are correctly set:
  * MAXELEROSDIR
  * MAXCOMPILERDIR

This release has been built with MaxelerOS 2013.2.1.

To compile the application, run:  
	make

If you are using a MaxCompiler version greater than 2013.2.1, you may need to
remove the distributed maxfiles before recompiling the application. In that
case, the following command before compilation:  
	make distclean

Makefile targets
================
  install  
  &ensp;&ensp;Compiles and installs all binaries (to bin/) and libraries (to lib/)  
  clean  
  &ensp;&ensp;Removes results of compilation from build directories  
  distclean  
  &ensp;&ensp;Removes all results of comakempilation from build directories, including
    all maxfiles


Also note that different project run rules can be activated through the
RUNRULE environment variable.  The default is to build for Vectis DFEs,
which can be explicitly targeted:  
&ensp;&ensp;&ensp;&ensp;make RUNRULE=Vectis install  
&ensp;&ensp;&ensp;&ensp;make RUNRULE=Vectis clean  
&ensp;&ensp;&ensp;&ensp;make RUNRULE=Maia  install  
&ensp;&ensp;&ensp;&ensp;make RUNRULE=Maia  clean
