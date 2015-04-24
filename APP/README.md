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
	Compiles and installs all binaries (to bin/) and libraries (to lib/)
  clean
	Removes results of compilation from build directories
  distclean
    Removes all results of comakempilation from build directories, including
    all maxfiles


Also note that different project run rules can be activated through the
RUNRULE environment variable.  The default is to build for Vectis DFEs,
which can be explicitly targeted:
	make RUNRULE=Vectis install
	make RUNRULE=Vectis clean
	make RUNRULE=Maia  install
	make RUNRULE=Maia  clean
