// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// this files contains addtions to third-party Classes


// this is more of a hack but works - it calculates the end time of the last note in a score

+ CtkScore {

	lastNoteEndTime {

		var parentNotes = this.notes;    // the notes of the parent-score
		var childrenNotes = Array.new;    // the notes of the child-score

		// get the children notes recursively
		var function = { arg children;
			children.do{
				arg score;
				childrenNotes = childrenNotes.add(score.notes);
				if (score.ctkscores.notNil) {
					function.value(score); // recursion
				}
			}}.value(this.ctkscores);

		var endTime = (parentNotes ++ childrenNotes).collect{
			arg i; i.duration + i.starttime}.maxItem;

		if (endTime.isNil) {endTime = 0}; // if there are no notes return 0

		^endTime;
	}
}
