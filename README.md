# Ambient Walk (with LibPd)
This is the android project for 'Ambient Walk' app using LibPd for data sonification, writtern in java and eclipse.

The app uses a microphone and an accelerometer of your mobile phone to detect your breathing period and walking pace during your walking meditation. It then generate responsive sound effect based on your walking data to foster your meditation practice.

#Latest Change (4th Commit)
Changed the sonification for breathing period in ambientwalk.pd.
Edited the threshold for peak detection. 

#3rd Commit
Added attack/decay envelope to represent breathing period in Ambientwalk.pd. 

#2nd Commit
Optimized breathing detection and sound effect in ambientwalk.pd.

#1st commit
The original 'Ambient Walk' code, with optimized unsupervised learning algorithm for activity detection (self-updating the threshold for peak detection based on individual activities). 
