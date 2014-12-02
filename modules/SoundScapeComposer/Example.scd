// full Example
~ssc = SoundScapeComposer.new("cars birds Bulgaria footsteps", 240, FoaDecoderMatrix.newStereo, 3, 100,false,200,10,10,1)

~ssc = SoundScapeComposer.new("Berlin birds cars footsteps", 240, FoaDecoderMatrix.newStereo, 3, 100,false,200,10,10,1)

~ssc.bounce()


r = ~ssc.runner

r.bounce


~ssc.interpreter.memory.keysValuesDo{arg k,v;
	[k, v].postln
}


~ssc.interpreter.memory.keysValuesDo{arg k,v;
	a = a.add([k, v["occurences"]])
}

a = a.sort{arg a,b; a[1][0][0] < b[1][0][0]}

a.do{arg i; i.postln}


~db = ~ssc.organicDb

~db.atmos[0]["title"]
~db.atoms.do{arg i; (i.collect{arg i; i["title"]}).postln;}

~db.atoms.do{arg i; i[0]["title"].postln}
~db.sequences.do{arg i; i["title"].postln}


~db.atoms[1][0]["frequency"]

~ss = ~ssc.sonicSpace

~ssc.sonicSpace[0].soundSources.do{arg i,j; j.postln; i.postln; i.exportScore}


// if I evaluate 3 times it works ????

s.boot

~ssc.sonicSpace[0].soundSources[12].exportScore

s.meter

// why so many atoms ??
// why not everything is downloaded?


~organicDb = SSC_OrganicDb("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeComposer/test/");
~atomsPath = "/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeComposer/test/atoms.json";
SSC_Acquire.acquireDataFile("cars birds Bulgaria footsteps",["%2BtypeOfSoundObject_Atom"],100, path: ~atomsPath, doneFunction: {
	"SSC: Atoms retrieved".postln;
	~organicDb.addData(~atomsPath);
});

~organicDb.prepare(20,10,1);
~organicDb.analyze;

~organicDb.atoms.do{arg i; i.do{arg j;
	j["rangeOfMovement"].postln
}}

~organicDb.rawData.do{arg i;
	i["rangeOfMovement"].postln
}


~organicDb.rawData.do{arg i;
	i["classes"].do{arg j;
		j["facetId"].postln;
	};
}


~organicDb.atoms.do{arg i; i.do{arg j; j["title"].postln; j["class"].postln}}


~organicDb.atoms.do{arg i; i.do{arg j; PathName(j["filePath"]).isFile.postln}}

~interpreter = SSC_Interpreter(~organicDb);

~sonicSpace = [
				~interpreter.interpret(240, 2, 200, false);
];

~runner = SSG_Runner(
	~sonicSpace,  // sonicSpace
	SSG_FixedListener(0@0@2,200*1.5), // a new static listener
	FoaDecoderMatrix.newStereo  // the decoder
);


~runner.bounce



	var filePath = (organicDb.auxilaryFolderPath ++ "Audio Bounce " ++ Date.getDate.asSortableString ++ "_" ++ UniqueID.next.asString).asString ++ ".aiff";
		// filePath.postln;
		fork {
			readyCond.wait;
			runner.bounce(duration,filePath);
		}


a = ~ssc.organicDb

~ss = ~ssc.sonicSpace


// Example

~path = (Document.current.dir ++ "/organicDb.json");

// acquire data
SSC_Acquire.acquireDataFile("cars birds London footsteps",nil,1000, path:~path, doneFunction: {
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});

// launch SSC
~ssc = SoundScapeComposer.new(~path, 240, FoaDecoderMatrix.newStereo, 2, 100,false,200,10,10,1)

~ssc.bounce()


~ssc.sonicSpace[0].soundSources.do{arg i; i.exportScore}

~organicDb = SSC_OrganicDb(~path);
~organicDb.prepare(200,10,10,1);

~organicDb.completions.size


~data["documents"].do{arg i;
	[ i["title"], i["typeOfSoundObject"], i["description"]].postln;
}


// check entries
~organicDb.entries.size
~organicDb.sequences.size
~organicDb.atoms.size
~organicDb.atmos.size
~organicDb.groupedSequences.size
~organicDb.groupedAtoms.size


~organicDb.sequences.do{arg i,j; j.postln; i.keysValuesDo{arg k,v;
	[k,v].postln;
}}

~organicDb.atoms.choose.keysValuesDo{arg k,v;
	[k,v].postln;
}



// new
~path = (Document.current.dir ++  "/data.json");
~db = SSC_Acquire.acquireDataFile("cars birds London",nil,50, path:~path, doneFunction: {
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
})

~data.keysValuesDo{arg k,v;
	[k,v].postln;
}

~data["documents"].size // Array of Dictionaries
~data["query"]  // Dictionary


a = ~data["documents"][0]["url"]++ ~data["documents"][0]["title"];
a = a.asUnixPath

("curl -o temp.wav " ++ a.asUnixPath).unixCmd


~data["documents"][0].keysValuesDo{arg k,v;
	[k,v].postln;
}

~data["documents"][1]["classes"].do{arg k;
	k["additionalInfo"].postln;
}

a = ~data["documents"][0]["classes"].collect{arg i; i["additionalInfo"]}

a = SSC_Parser(~path)
a.sequences.size
a.atoms.size


// manual retrieval
~path = (Document.current.dir ++  "/data.json");
("curl \"https://dev.celi.it/soda/api/soda/extended/documents.json?type=search&start=0&end=10&defaultOperator=AND&sortField=date_doc&sortDir=-1&searchText=%22british+bird%22&classPath=typeOfSoundObject_Atom\" > " ++ ~path.asUnixPath).unixCmd({
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});

~data

~data.keysValuesDo{arg k,v;
	[k,v].postln;
}

~path = (Document.current.dir ++  "/data.json");
("curl \"https://dev.celi.it/soda/api/soda/extended/dedupDocuments.json?type=search&start=0&end=20&classPath=&defaultOperator=AND&sortDir=-1&searchText=footsteps\" > " ++ ~path.asUnixPath).unixCmd({
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});

~path = (Document.current.dir ++  "/data.json");
("curl \"https://dev.celi.it/soda/api/soda/extended/dedupDocuments.json?type=search&start=0&end=20&classPath=&defaultOperator=AND&sortDir=-1&searchText=footsteps%20london\" > " ++ ~path.asUnixPath).unixCmd({
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});


~data["documents"].do{
	arg i;
	[i["title"], i["description"]].postln;
}

~data["documents"].do{
	arg i;
	[i["title"]].postln;
}

~data["documents"].size

~data["documents"].keysValuesDo{arg k,v;
	[k,v].postcs;
}