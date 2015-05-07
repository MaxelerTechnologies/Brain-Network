# BrainNetwork

<img src="http://appgallery.maxeler.com/v0.1/app/Brain%20Network/icon" alt="Brain Network">

## Description

This App implements a linear correlation analysis of brain images to detect brain activity. It automatically extract a dynamic network from brain activity of slices of mice brain. It is interesting to analyse the long range interactions between the different brain hemispheres. Moreover, this should be done in real-time in order to actively collect information and to suggest action to take to the scientists.

## Content

The repo root directory contains the following items:

- APP
- DOCS
- LICENCE.txt

### APP

Directory containing project sources.

### DOCS

Documentation of the project.
  
### LICENSE.txt

License of the project.

## Information to compile

Ensure the environment variables below are correctly set:
  * `MAXELEROSDIR`
  * `MAXCOMPILERDIR`

To compile the application, run:

    make RUNRULE="<ProfileName>"

If would like to remove the distributed maxfiles before recompiling the application run the following command before compilation:

    make RUNRULE="<ProfileName>" distclean

## Makefile targets

### build  

Compiles the application

### clean  

Removes results of compilation from build directories  

### distclean  

Removes all results of comakempilation from build directories, including all maxfiles

Brain Network on [AppGallery](http://appgallery.maxeler.com/)   
