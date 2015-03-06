// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SoundZone


SSG_SoundZone {

	// ==================== public class variables ===================

	// classvar < group;       // (CtkAudio) a group used to ensure proper order of execution 
	classvar counter = 0;  // (Integer) global counter used to created unique IDs

	// ==================== public instance variables ===================
	
	var < bounds;           // (Cartesian) the shape of the zone (currently rectangular only) in x,y,z format
	var < position;         // (Cartesian) the positioning of the zone in space
	// var < bus;              // (CtkAudio) its soundZone is associated with a bus
	var < soundSources;           // (Array of SSG_AbstractSonicSources) - at least one should SSG_SonicAtmosphere should be present - the list of the sounds events/sources occuring in the space
	// var < subZones;         // (SSG_soundZone) other sub-zones
	var < reverbProfile;    // (??) reverbaration profile
	var < resonanceProfile; // (Array of Float) an array of frequency coefficients that define the resonant qualities of the zone 
	var < opacity;          // (Float) -> 1 means that the zone is a perfect sound insultator and therefore sound cannot travel out of this zone, 0 the exact opposite
	var < filterSlope;    // (Integer) a filter is used to dampen sounds intruding TO other zones, this is the slope of the filter
	// var < filterProfile;    // (Symbol) -> the type of algorithm used for the localization of the sounds inside the zone. \flat means that localization is only based on 
	// var < gain;             // (Float) a gain factor used to modulate the audiolevels of everything happening inside the zone
	var < id;               // (Integer) a unique id
	var < label;            // (Symbol) a user defined label
	var < info;             // (String) user-defined custom text

	// ===================== initClass  =========================
	
	*initClass {

	}

	// ===================== new/init =========================

	*new {

		arg                  // expected arguments:
		bounds = 10@10@10,   // (Cartesian) the size of the sound-zone
		position = 0@0@0,    // (Cartesian) the position of the zone in space
		soundSources,         // (Array of SSG_AbstractSonicSources) - at least one should SSG_SonicAtmosphere should be present - the list of the sounds events/sources occuring in the space
		opacity = 1,              // (Number) -> 1 means that the zone is a perfect sound insultator and therefore sound cannot travel in or out of this zone, 0 the exact opposite
		filterSlope = 1,         // (Integer) a low-pass filter is used interally to dampen sounds coming from other zones, this is the slope of the filter (value should be 1-4)
		reverbProfile = [100,0.3,0.8,0.5,0.3], // (Array of 5 Floats) roomSize, reverbTime, reverbDamp, earlyMix,lateMix
		resonanceProfile = [0], // (Array of Float) an array of frequency coefficients that define the resonant qualities of the zone 		
		label = \no,         // (Symbol) a custom name for the object
		info = "";           // (String) a custom info-text for object

		^super.new.pr_init(bounds, position, soundSources, opacity, filterSlope, reverbProfile, resonanceProfile, label, info);
	}

	pr_init{
		arg bounds_, position_, soundSources_, opacity_, filterSlope_, reverbProfile_, resonanceProfile_, label_, info_;

		// ===================== type checking  =========================
		case
		{bounds_.class != Cartesian} { Error("SSG_SoundZone: 'bounds' argument should be an instance of Cartesian").throw }
		{position_.class != Cartesian} { Error("SSG_SoundZone: 'position' argument should be an instance of Cartesian").throw }
		{soundSources_.class != Array } { Error("SSG_SoundZone: 'soundSources' argument should be an instance of Array containing SSG_AbstractSoundSoureces objects").throw }		
		{soundSources_.every(_.isKindOf(SSG_AbstractSonicSource)).not} { Error("SSG_SoundZone: 'soundSources' argument should be an instance of Array containing SSG_AbstractSonicSource objects").throw }		
		{soundSources_.any(_.isKindOf(SSG_SonicAtmosphere)).not} { Error("SSG_SoundZone: 'soundSources' should contain at least one instance of SSG_SonicAtmosphere.").throw }		
		{opacity_.isKindOf(Number).not} { Error("SSG_SoundZone: 'opacity' should be an instance of Number.").throw }		
		{filterSlope_.isKindOf(Integer).not} { Error("SSG_SoundZone: 'filterSlope' should be an instance of Integer.").throw }		
		{(filterSlope_ < 1) || (filterSlope_ > 4)} { Error("SSG_SoundZone: 'filterSlope' should be of value 1,2,3 or 4.").throw }		
		// {reverbProfile_.class != Symbol} { Error("SSG_SoundZone: 'reverbProfile' argument should be an instance of Symbol").throw }
		// {resonanceProfile_.class != Symbol} { Error("SSG_SoundZone: 'resonanceProfile' argument should be an instance of Symbol").throw }
		{label_.class != Symbol} { Error("SSG_SoundZone: 'label' argument should be an instance of Symbol").throw }
		{info_.class != String} { Error("SSG_SoundZone: 'info' argument should be an instance of String").throw };

		// generate and assign a unique id
		counter = counter + 1;
		id = counter;    
		
		// init some variables
		label = label_;
		info = info_;
		bounds = bounds_;
		position = position_;
		soundSources = soundSources_;
		opacity = opacity_;
		filterSlope = filterSlope_;
		reverbProfile = reverbProfile_;
		resonanceProfile = resonanceProfile_;
		// bus = CtkAudio(4);
	}

	// ===================== setters  =========================

	setLabel {

		arg label_; 
		
		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_SoundZone \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_SoundZone \(id:" ++ " " ++ id ++ "\): label has been succesfully set to:" + label ++ ".").postln;
		}

		^this;
	}
	
	setInfo {
		
		arg info_; 
		
		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_SoundZone \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_SoundZone \(id:" ++ " " ++ id ++ "\): Info has been succesfully set to:" + info).postln;
		}

		^this;
	}


	// ===================== public methods  =========================

	
	addSoundSource{ arg soundSource;

		if (soundSource.isKindOf(SSG_AbstractSonicSource).not) { 
			Error("SSG_SoundZone \(id:" ++ " " ++ id ++ "\): 'soundSource' argument should be an instance of SSG_AbstractSonicSource").throw 
		} {
			soundSources = soundSources.add(soundSource);
		}
		
		^this;
	}

	isWithinZone{ arg locus; // asks whether a locus (Should be a Cartesian) is within the zone
		if (
			((locus.x>=position.x)&&(locus.x<=(position.x + bounds.x)))&&
			((locus.y>=position.y)&&(locus.y<=(position.y + bounds.y)))&&
			((locus.z>=position.z)&&(locus.z<=(position.z + bounds.z)))
		) {^true} {^false}
	}
}


