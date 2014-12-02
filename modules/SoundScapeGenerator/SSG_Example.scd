// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2014

// this file is part of the Sound Design Accelerator (SoDA) project

s.boot

().play

~runner.bounce(240);

(
// Server.default.waitForBoot({
// sonic objects
~path = PathName((PathName(thisProcess.nowExecutingPath).pathOnly ++  "/../sample files/"));
~sonicObjects = Dictionary.new;
~path.filesDo{arg path;
	~sonicObjects[path.fileNameWithoutExtension.asSymbol] = SSG_SonicObject(path.fullPath,disk:true);
};

// Sonic Sources
~sonicSources = [
	// birds
	SSG_FixedSound(
		~sonicObjects['SoDA_BritishBirds BBC12_04__00'],
		30,  0.1,  0.4, 5@1@3
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_BritishBirds BBC12_05__01'],
		140,  0.1, 0.4, (-2)@5@2
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_BritishBirds BBC12_05__02'],
		170,  0.1, 0.4, 10@(-2)@2
	),
	SSG_AmbulatorySoundEnv(
		~sonicObjects['SoDA_BritishBirds BBC12_13__01'],
		0,  0.1, 0.3,   [
			Env([0,8,2],[10,20]),
			Env([2,-4],[30]),
			Env([0,4],[30]),
		]
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_BritishBirds BBC12_13__02'],
		70,  0.1, 0.4, 5@5@1
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_BritishBirds BBC12_14__01'],
		50,  0.1, 0.4, (-2)@(-10)@3
	),

	// cars
	SSG_FixedSound(
		SSG_Sequence(Prand([
			~sonicObjects['SoDA_Cars BBC16_34__00'],
			~sonicObjects['SoDA_Cars BBC16_32__00'],
			~sonicObjects['SoDA_Cars BBC16_33__00'],
		],inf),Pwhite(8,15,4)),
		Pseq([0,120,240],1),  0.1,  0.4, 5@(-10)@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_19__00'],
		30,  0.1,  0.6, 6@7@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_20__00'],
		70,  0.1, 0.6, 6@10@1
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_22__00'],
		6,  0.1, 0.4,  6@7@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_47_1__00'],
		15,  0.1, 0.5,  2@3@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_54__00'],
		134,  0.1, 0.7,  20@3@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_55__00'],
		32,  0.1,0.7,  2@6@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_51__00'],
		112,  0.1, 0.7, 2@6@0
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_52__00'],
		160,  0.1, 0.7,  4@7@0.5
	),
	SSG_FixedSound(
		~sonicObjects['SoDA_Cars BBC16_53__00'],
		87,  0.1, 0.7, 2@3@0.5
	),
	SSG_FixedSound(
		SSG_Sequence(Prand([
			~sonicObjects['SoDA_Cars BBC16_48__01'],
			~sonicObjects['SoDA_Cars BBC16_48__02']
		],inf),Pwhite(0.1,0.2,4)),
		Pseq([5,100],1),  0.1, 0.5, 1@1@0
	),

	// footsteps
	SSG_AmbulatorySoundEnv(
		~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__01'],
		20,  0.1, 0.3,  [
			Env([-10,10],[30]),
			Env([2,-7],[30]),
			Env([-1,-1],[30]),
		]
	),
	SSG_AmbulatorySoundEnv(
		SSG_Sequence(
			Prand([
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__02'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__42'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__12'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__05'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__09'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__19'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__29'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__31'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__32'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__20'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_01_1__40'],
				nil],inf),
			Prand([0.3],50),
		),
		Pseq([30,150],1),  0.1, 0.3,  [
			Env([-5,2],[30]),
			Env([1,-10],[30]),
			Env([-1,-1],[30]),
		]
	),
	SSG_AmbulatorySoundEnv(
		~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__01'],
		100,  0.1, 0.3,  [
			Env([-10,1],[30]),
			Env([-2,5],[30]),
			Env([-1,-1],[30]),
		]
	),
	SSG_AmbulatorySoundEnv(
		SSG_Sequence(
			Prand([
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__02'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__42'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__12'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__05'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__09'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__19'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__29'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__31'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__32'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__20'],
				~sonicObjects['SoDA_FootstepsConcrete 6016_10_1__40'],
				nil],inf),
			Prand([0.3],50), 0.01
		),
		Pseq([45,200],1),  0.1, 0.3,  [
			Env([-5,6],[30]),
			Env([7,-15],[30]),
			Env([-1,-1],[30]),
		]
	),

	// atmo
	SSG_SonicAtmosphere(
		SSG_Sequence(Pseq([
			~sonicObjects['SoDA_CzechRepublicCity WSS02_08__00'],
			~sonicObjects['SoDA_CzechRepublicBusSt WSS02_06__00'],
			~sonicObjects['SoDA_CzechRepublicPark WSS02_17__00'],
		]),Pseq([100,130,50],inf),20),0.6,
	)
];

// sonic space, etc
~listener = SSG_FixedListener(0@0@2,100);
// ~listener = SSG_AmbulatoryListenerEnv(
// 	[
// 		Env([-50,20],[240]),
// 		Env([50,-10],[240]),
// 		Env([1,1],[240]),
// 	],100);
~zone1 = SSG_SoundZone(100@100@10,0@0@0,~sonicSources, 1, 1,
	[100,0.01,0.7,0.1,0.05] // reverb profile for an exterior place
);
// ~listener = SSG_FixedListener(0@(-10)@(-20),100);
// ~zone1 = SSG_SoundZone(100@100@10,0@0@0,~sonicSources, 0.4, 4,
// 	[100,0.2,0.7,0.2,0.05] // reverb profile for an exterior place
// );
~decoder = FoaDecoderMatrix.newStereo;
~runner = SSG_Runner([~zone1],~listener, ~decoder);
// })
)

s.boot

~runner.play(0);

~runner.stop

s.makeGui

s.queryAllNodes

~runner.bounce(240);
~runner.bounce(60);


a.score
a.saveToFile("~/Desktop/test.score")


s.makeGui


a = SSG_Sequence(Prand([
	~sonicObjects['SoDA_Cars BBC16_48__01'],
	~sonicObjects['SoDA_Cars BBC16_48__02']
],inf),Pwhite(0.1,0.2,4))


a.play(0)

a.stop









// old example


(
Server.default.waitForBoot({
	// city atmosphere
	~cityAmbience = SSG_SonicAtmosphere(
		SSG_SonicObject(   // sequence will be generated automatically given just a vertex
			"/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/city.aif")
	);
	~bird1 = SSG_FixedSound(
		SSG_SonicObject("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/bird1.wav"),
		Pseq([0,15],1),  0.1, 0.4,  40@1@20
	);
	~bird2 = SSG_FixedSound(
		SSG_SonicObject("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/bird2.wav"),
		Pseq([8,40],1),  0.1, 0.4,  -10@5@5
	);
	~car = SSG_AmbulatorySoundEnv(
		SSG_SonicObject(	"/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/car1.wav"),
		15, 4, 0.7, [
			Env([0,100,50],[30,20]),
			Env([100,-14],[50]),
			Env([2,2],[50]),
		]
	);
	~dog =  SSG_AmbulatorySoundEnv(
		SSG_Sequence(
			// pSonicObject
			Prand([
				SSG_SonicObject("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/dog1.wav"),

				SSG_SonicObject("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/dog2.wav"),
				SSG_SonicObject("/Users/marinos/projects/ongoing/SoDA/SoDa SC prototype/modules/SoundScapeGenerator/src/test sound files/dog3.wav"),
			],4),
			Prand([1,2],2)
		),
		Pseq([5,10,20],1),0.1,0.5,
		[
			Env([0,50,-10],[5,20]),
			Env([5,-14],[40]),
			Env([0.5,0.5],[50]),
		]
	);
	~listener = SSG_FixedListener(0@0@2,100);
	~zone1 = SSG_SoundZone(500@500@10,0@0@0, [
		~cityAmbience, ~bird1, ~bird2, ~dog, ~car
	]);
	~decoder = FoaDecoderMatrix.newStereo;
	~runner = SSG_Runner([~zone1],~listener, ~decoder);
})
)

~runner.play(0);
// or ~runner.bounce;

a = ~runner.bounce(60);


s.reboot


