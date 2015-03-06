
// !!! REMEMBER TO CHANGE THE WAY FILEPATHS ARE SET BEFORE DEPLOYMENT TO THE SERVER

// full Example
~ssc = SoundScapeComposer.new("cars birds Berlin", 240, \stereo, 3, 100)

~ssc = SoundScapeComposer.new("country day", 180, \stereo, 3, 200, "path?")

~ssc.bounce

// check runner and proceed

a = ~ssc.organicDb

a = ~ssc.sonicSpace

a.air[0]["title"]

a.atmos[2].keysValuesDo{arg k,v; [k,v].postln;}

a.atoms.do{arg i; 
	i.do{arg j; j["title"].postln}
}

a.atmos.do{arg i; 
	i["title"].postln
}

a.sequences.do{arg i; 
	i["title"].postln
}

// btw
SoDA_BirdWingFlap JBA01_90__01, 02, 03 etc -> are atoms not sequences

a = ~ssc.organicDb
a.entries.size

a.atoms.size
a.atmos.size
a.air.size
a.sequences.size


a.atmos[0]["filePath"]

[].isKindOf(Array)

a.atmos[2]["relativeAmplitude"]

a = "ftp://91.212.167.101/Editing_v01/2_Foreground/1b_GenericSoundsSFX/"

a[a.size-1]==$/


a.entries.do{arg item; 
	// simplify some tags
	if (item["typeOfSoundObject"].isKindOf(Array)) {
		item["typeOfSoundObject"] = item["typeOfSoundObject"][0];
	};
	if (item["typeOfSpace"].isKindOf(Array)) {
		item["typeOfSpace"] = item["typeOfSpace"][0];
	};
}

a.sequences.size
a.air.size
a.atmos.size
a.atoms.size

a.air[0].keysValuesDo{arg k,v;
	[k,v].postln;
}

a.atoms[0].[0].keysValuesDo{
	arg k,v; 
	[k,v].postln;
}

// == test new API ===

~path = (Document.current.dir ++  "/data.json");
("curl \"https://dev.celi.it/soda/api/soda/extended/dedupDocuments.json?type=search&start=0&end=50&classPath=&sortDir=-1&searchText=cars%20birds%20Berlin\" > " ++ ~path.asUnixPath).unixCmd({
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});

~path = (Document.current.dir ++  "/data.json");
("curl \"https://dev.celi.it/soda/api/soda/extended/dedupDocuments.json?type=search&start=0&end=50&defaultOperator=OR&sortDir=-1&searchText=country%20day\" > " ++ ~path.asUnixPath).unixCmd({
	"ok".postln;
	~data = JSONFileReader.read(~path)["result"];
});


~data.keysValuesDo{arg k,v;
	[k,v].postln;
}

~data["documents"][0]

.size

~data["documents"][0].keysValuesDo{arg k,v;
	[k,v].postln;
}

["relativeAmplitude"]


~data["atoms"]

~data["documents"].do{
	arg i;
	[i["title"], i["typeOfSoundObject"]].postln;
}


~data["documents"][3].keysValuesDo{arg k,v;
		[k,v].postln;
	}

~data.keysValuesDo{arg k,v; 
	[k,v].postln;
}

~data["result"].keysValuesDo{arg k,v; 
	[k,v].postln;
}

~data["documents"].do{
	arg i;
	[i["title"], i["url"]].postln;
}


