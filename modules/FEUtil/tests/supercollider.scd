// Batch Processing Utility for SoDA project
// by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)

// *** under developement ***

(
fork {
	var resultbuf, resultbuf2, resultpath, oscpath, score, dur, sf, cond, size, data;

	// get duration
	sf = SoundFile.openRead(Platform.resourceDir +/+ "sounds/a11wlk01.wav");
	dur = sf.duration;

	sf.close;

	// naming
	resultpath = PathName.tmp +/+ UniqueID.next ++ ".aiff";
	oscpath = PathName.tmp +/+ UniqueID.next ++ ".osc";

	// create score
	score = Score([
		[0, (resultbuf = Buffer.new(s, 1000, 4, 0)).allocMsg], // maximum numFrames (should be calculated by unit of time)
		[0, [\d_recv, SynthDef(\analysis, {
			var sig = SoundIn.ar(0), // will come from NRT input file
			fft = FFT(LocalBuf(2048, 1), sig),
			i = PulseCount.kr(Impulse.kr(1)), // trigger every second
			analysis1 = [ 
				SpecCentroid.kr(fft), // spectral centroid
				SpecFlatness.kr(fft), // spectral flatness
				SpecPcile.kr(fft,0.8), // 80-SpecPcile
				FFTCrest.kr(fft) // spectral crest
			]; 
			// analysis2 = [
			// 	SensoryDissonance.kr(fft), // Sensory Dissonance
			// 	FFTSpread.kr(fft),
			// 	FFTSlope.kr(fft),
			// 	DC.kr(0)
			// ];
			// 'i' must be audio-rate for BufWr.ar
			BufWr.kr(analysis1, resultbuf, i, loop: 0);
			BufWr.kr(i, resultbuf, DC.kr(0), 0); // # of points in index 0
		}).asBytes]],
		[0, Synth.basicNew(\analysis, s, 1000).newMsg], // this too !
		[dur, resultbuf.writeMsg(resultpath, headerFormat: "AIFF", sampleFormat: "float")]
	]);

	cond = Condition.new;

	// now calculate the rest

	// osc file path, output path, input path - input is soundfile to analyze
	score.recordNRT(oscpath, "/dev/null", sf.path, sampleRate: sf.sampleRate,
		options: ServerOptions.new
		.verbosity_(-1)
		.numInputBusChannels_(sf.numChannels)
		// .numOutputBusChannels_(sf.numChannels)
		.numOutputBusChannels_(4)
		.sampleRate_(sf.sampleRate),
		action: { cond.unhang } // this re-awakens the process after NRT is finished
	);
	cond.hang; // wait for completion

	sf = SoundFile.openRead(resultpath);

	// get the size: one frame at the start
	size = FloatArray.newClear(4);
	sf.readData(size);
	// size = size[0];
	// now the rest of the data
	size.postln;
	size = size[0] * 4;
	data = FloatArray.newClear(size);
	sf.readData(data);
	sf.close;

	File.delete(oscpath); File.delete(resultpath);

	data.postln; // these are your onsets!
	// ~d = data.asArray;
	// ~d.reshape(8,100).postln;
};
)
