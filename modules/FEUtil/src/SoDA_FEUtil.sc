
// Feature Extraction Utility for SoDA project
// CIRMA, Univerista Degli Studi di Torino
// by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)

// *** under developement ***

// to do list:
// save data to an XML instead of plain text

// Note that currently Loudness tracking will work correctly only for files with a sampling rate of 44100 or 48000

FEUtil {

	// ----------------------- Instance Variables ------------------------

	// paths 
	var path;  // the path pointing at a signleton file or at a folder to be analyzed
	var tmpAudioPaths; // an array of temporary audio files used internally
	var tmpOscPaths; // an Array of temporary score file used internally

	// conditions to control the flow between various parts of the program
	var prepareCond;  // is prepared?
	var scoreCond;  // is score defined ?
	var nrtCond; // has nrt analysis finished ?
	var onsetsAnalysisCond; // has the onsets analysis finished ?
	var analysisResultsCond; // is the collectRestult finished ?
	var onsetsAnalysisResultsCond; // it the collectOnsetsAnalysisResults finished?
	var cleanCond; // has the cleaning ended ?
	
	// NRT analysis related
	var soundFile; // a soundFile to read paths (both during and after analysis)
	var duration; // the duration of the soundFile
	var score, onsetsScore; // the NRT scores
	var timeInterval; // the time interval between each subsequent measurment 
	var onsetsSensitivity; // the sensitivity of the onsets analysis (0-1 normally)
	var analysisBuffer; // an Array of Buffers used for holding the analysis data
	var analysisBufferSize; // the size of the resultBuffer (depends on timeInterval and input's file duration)
	var onsetsBuffer; // a special Buffer for the onsets detection
	var synthDefs;  // an Array of the various synthefs to use

	// post-analysis
	// var 

	// -------------------------- *new/init -----------------------------

	// *new { arg path, timeInterval=1, onsetsSensitivity=0.3;
	// 	^super.new.init(path, timeInterval, onsetsSensitivity);
	// }

	*analyze { arg path, timeInterval=1, onsetsSensitivity=0.3;
		^super.new.init(path, timeInterval, onsetsSensitivity);
	}

	init { arg path_, timeInterval_, onsetsSensitivity_;
		
		// pass arguments to instance variables
		path = path_; // copy path_ to path
		path = path.standardizePath; // standardize path
		timeInterval = timeInterval_;
		onsetsSensitivity = onsetsSensitivity_; 

		// print preampl
		"\n\nFEUtil: Feature Extraction Utility.\nPart of the Sound Design Accelator (SoDA) project.\nCIRMA Universita degli Studi di Torino.\nDeveloped by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it).\n\n".postln;
		// sanity tests
		if (path.isString.not) {Error("FEUtil: path is not a valid string !").throw};
		if (timeInterval.isNumber.not)  {Error("FEUtil: timeInterval is not a number!").throw};

		// redirect to the appropriate method
		if (PathName(path).isFile) {
			this.analyzeOne(path)
		} { 
			if (PathName(path).isFolder) {
				this.analyzeMany(path)
			} {
				Error("FEUtil: invalid path!").throw;
			}
		};

	}

	// -------------------------- analyzeOne ---------------------------

	analyzeOne{ arg filePath;

		var done = Condition.new; // analyzeOne will return a Condition so that analyzeMany can analyze a lot of files one after the other and not simultaneously
		
		path = filePath; // update Path

		// set all Conditions to false (usuful when analyzeMany will call analyzeOne several times)
		
		prepareCond = Condition.new;
		scoreCond = Condition.new;
		nrtCond = Condition.new;
		onsetsAnalysisCond = Condition.new;
		analysisResultsCond = Condition.new;
		onsetsAnalysisResultsCond = Condition.new;
		cleanCond = Condition.new;
		
		("FEUtil: launching FEUtil for" + path + "file").postln;

		// test if the file is a valid soundFile and skip if it isn't
		if ( ["aif","aiff","wav"].any{arg item; 
			item.compare(PathName(path).extension,true) == 0}
		) 
		{ // if so then analyze file
			fork { 
				this.prepare(path); // prepare the score 
				prepareCond.wait; // sync
				this.defineScore(); // define score
				scoreCond.wait; // sync
				this.runAnalysis(); // run Analysis
				nrtCond.hang; // wait until analysis is done
				this.runOnsetsAnalysis();
				onsetsAnalysisCond.hang; // wait until onsets analysis is done, too
				this.collectResults(); // collect results
				analysisResultsCond.wait; // sync
				this.collectOnsetsAnalysisResults();
				onsetsAnalysisResultsCond.wait;
				this.clean(); // cleaning
				cleanCond.wait; // sync
				("FEUtil: done analyzing" + path + "!").postln;
				// updateCondition
				done.test_(true).signal;
			};
		} {
			// else skip file
			("FEUtil: " ++ path + "is not recognized as an audio file and will be skipped.").postln;
			// updateCondition
			done.test_(true).signal;
		};
		^done;  // return a Condition so that we can chain several analysis if needed 
	}

	// -------------------------- analyzeMany ---------------------------
	analyzeMany{ arg path;
		fork { PathName(path).filesDo{arg file; 
			var done = this.analyzeOne(file.fullPath);
			done.wait;
		}};
	}

	// --------------------------- prepare ------------------------------

	prepare{ arg filePath;

		("FEUtil: preparing..").postln;

		// get file duration
		soundFile = SoundFile.openRead(filePath);
		duration = soundFile.duration;
		
		// check number of channels
		if (soundFile.numChannels!=1) {Error("FEUtil: Wrong number of channels; FEUtil only analyzes monophonic files").throw};

		soundFile.close; // close soundFile

		// create unique file names
		tmpAudioPaths = Array.fill(2,{
			PathName.tmp +/+ UniqueID.next ++ ".aiff";
		});
		tmpOscPaths = Array.fill(2,{
			PathName.tmp +/+ UniqueID.next ++ ".osc";
		});

		// calculate the maximum numFrames for the Buffer
		analysisBufferSize = (duration / timeInterval).ceil; 

		// update Condition
		prepareCond.test_(true).signal;
	}

	// ----------------------------- Score -----------------------------

	defineScore {

		("FEUtil: defining score..").postln;
		
		score = Score([
			// first the Buffer
			[0, (analysisBuffer = Buffer.new(Server.default, analysisBufferSize, 10)).allocMsg ], // a 10-channel Buffer

			//then the SynthDefs
			[0, [\d_recv, SynthDef(\FEUtilAnalysis, {
				var sig = SoundIn.ar(0); // will come from NRT input file
				var fft = FFT(LocalBuf(1024, 1), sig); // fft analysis
				var pulseCount = PulseCount.kr(Impulse.kr(timeInterval.reciprocal)); // triggers (every timeInterval)
				var pitch,hasPitch, analysis; 
				# pitch, hasPitch = Tartini.kr(sig);
				analysis = [ // analysis UGens herein
					pitch,
					hasPitch,
					Loudness.kr(fft),
					SpecCentroid.kr(fft), // spectral centroid
					SpecFlatness.kr(fft), // spectral flatness
					SpecPcile.kr(fft,0.8), // 80-SpecPcile
					FFTCrest.kr(fft), // spectral crest
					SensoryDissonance.kr(fft), // Sensory Dissonance
					FFTSpread.kr(fft) * 0.000001,
					FFTSlope.kr(fft)
				];
				BufWr.kr(analysis, analysisBuffer, pulseCount, loop: 0); // write analysis data
				BufWr.kr(pulseCount, analysisBuffer, DC.kr(0), 0); // at index 0 write the pulse count so that the number of measurements is known
			}).asBytes]],

			// start the Synth
			[0, Synth.basicNew(\FEUtilAnalysis, Server.default, analysisBufferSize).newMsg], 

			// write analysis data to temporary files
			[duration, analysisBuffer.writeMsg(tmpAudioPaths[0], headerFormat: "AIFF", sampleFormat: "float")]
		]);

		onsetsScore =  Score([
			
			[0, (onsetsBuffer = Buffer.new(Server.default, soundFile.numFrames, 1)).allocMsg ], // this is a special buffer for the onsets and therefore should be monophinic and bigger ** maybe it is a lot bigger than needed, but there is no way to know in advance how many onsets will be found so it's better to be sure - note however that this might slow up the whole algorithm significantly 
			// onsets analysis  
			[0, [\d_recv, SynthDef(\FEUtilOnsetsDetection, {
				var sig = SoundIn.ar(0); // will come from NRT input file
				var fft = FFT(LocalBuf(1024, 1), sig); // fft analysis
				// var trigger = Onsets.kr(fft);
				var trigger = Onsets.kr(fft,onsetsSensitivity);
				var pulseCount  = PulseCount.kr(trigger); // count the triggers:
				var timer = Sweep.kr(1); // count time in seconds
				BufWr.kr(timer, onsetsBuffer, pulseCount, loop: 0);
				BufWr.kr(pulseCount, onsetsBuffer, DC.kr(0), 0); // at index 0 write the pulse count so that the number of measurements is known
			}).asBytes]],
			
			// start the Synth
			[0, Synth.basicNew(\FEUtilOnsetsDetection, Server.default, analysisBufferSize).newMsg], 

			// write analysis data to temporary files
			[duration, onsetsBuffer.writeMsg(tmpAudioPaths[1], headerFormat: "AIFF", sampleFormat: "float")]
		]);

		// update Condition
		scoreCond.test_(true).signal;
	}	

	// --------------------------- run analysis -------------------------

	runAnalysis {
		
		("FEUtil: analyzing file and extracting features...").postln;
		
		score.recordNRT(tmpOscPaths[0], "/dev/null", soundFile.path, sampleRate: soundFile.sampleRate,
			options: ServerOptions.new
			.verbosity_(-1)
			.numInputBusChannels_(1)
			.numOutputBusChannels_(10)
			.sampleRate_(soundFile.sampleRate),
			action: { nrtCond.test_(true).signal;} // update Condition
		);
	}
	
	runOnsetsAnalysis {

		onsetsScore.recordNRT(tmpOscPaths[1], "/dev/null", soundFile.path, sampleRate: soundFile.sampleRate,
			options: ServerOptions.new
			.verbosity_(-1)
			.numInputBusChannels_(1)
			.numOutputBusChannels_(1)
			.sampleRate_(soundFile.sampleRate),
			action: { onsetsAnalysisCond.test_(true).signal } // update Condition
		);
	}

	// ----------------------------- Results -----------------------------

	collectResults{

		var size; // the size of each dataSet (used internally)
		var data; // a generic raw data container 
		var file; // the file to put the data inside
		var typology; // the typology of the analysis 

		("FEUtil: saving the analysis results..").postln;

		// create the typology
		// typology = ["Pitch: ","Loudness: ","Centroid: ","Complexity: ",
		// 	"WeightedSpectralMaximum: ","Sharpness: ","Dissonance: ",
		// 	"Spread: ","Slope: ","Onsets: "];
		
		typology = ["Pitch;","Loudness;","Centroid;","Complexity;",
			"WeightedSpectralMaximum;","Sharpness;","Dissonance;",
			"Spread;","Slope;","Onsets;"];
		
		// openRead temporary file
		soundFile = SoundFile.openRead(tmpAudioPaths[0]);
		
		// read size from file
		size = FloatArray.newClear(10); // an empty FloatArray
		soundFile.readData(size); // read data to array
		size = size[0] * 10; // because there are 10 channels
		
		// read data from file
		data = FloatArray.newClear(size); // an empty FloatArray
		soundFile.readData(data); // read data into the array
		soundFile.close; // close soundFile

		// preprocess and re-structure dataset
		data = data.as(Array);  // convert to Array
		data = data.collect(_.round(0.01)); // round to the second decimal
		data = data.reshape( (data.size/10).asInteger, 10).flop; // re-organize data
		
		// check is pitch has been indeed tracked properly and zero-out all uncertain pitches
		data[1].do{ arg hasPitch, index;
			if (hasPitch<0.9) {  // if hasPitch is less than 0.9
				data[0][index] = 0; // zero out tracked pitch
			}
		};
		
		// remove hasPith from dataset
		data.removeAt(1);
		
		// put everything in a file AND print to the post window
		
		// file = File(path ++ ".analysis","w"); // open for writing
		// data.do{ arg data,index;
		// 	(typology[index] + data.asString).postln;
		// 	file.write(typology[index]);
		// 	file.write(data.asString);
		// 	file.write("\n");
		// };

		file = File(path ++ ".csv","w"); // open for writing
		data.do{ arg data,index;
			(typology[index] + data.asString).postln;
		};
		typology.do{arg item; file.write(item)};
		file.write("\n");
		data.do{arg item; file.write(item.asString); file.write(";")};
		file.close;  // close file when done

		// update Condition
		analysisResultsCond.test_(true).signal;
	}


	// ----------------------------- Onsets Results ----------------------


	collectOnsetsAnalysisResults {


		var size; // the size of each dataSet (used internally)
		var data; // a generic raw data container 
		var file; // the file to put the data inside

		// openRead temporary file
		soundFile = SoundFile.openRead(tmpAudioPaths[1]);

		// read size from file
		size = FloatArray.newClear(1); // an empty FloatArray
		soundFile.readData(size); // read data to array
		size = size[0]; // that many onsets found
		
		// read data from file
		data = FloatArray.newClear(size); // an empty FloatArray
		soundFile.readData(data); // read data into the array
		soundFile.close; // close soundFile

		// preprocess and re-structure dataset
		data = data.as(Array);  // convert to Array
		data = data.collect(_.round(0.001)); // round to the third decimal
		
		// put onsets in the file AND print to the post-window
		// file = File(path ++ ".FEUtil.analysis","a"); // open for appending
		("Onsets: " ++ data.asString).postln;
		
		// file.write("Onsets: ");
		// file.write(data.asString);
		// file.close;  // close file when done

		file = File(path ++ ".csv","a"); // open for appending
		file.write(data.asString);
		file.write(";");
		file.close;  // close file when done

		onsetsAnalysisResultsCond.test_(true).signal;
	}

	// ----------------------------- clean  -----------------------------
	
	clean {
		("FEUtil: removing temporary files..").postln;
		
		// removing temporary files;
		tmpOscPaths.do { arg file; 
			File.delete(file); 
		};
		tmpAudioPaths.do { arg file; 
			File.delete(file); 
		};

		// update Condition
		cleanCond.test_(true).signal;
	}

} // end of file

