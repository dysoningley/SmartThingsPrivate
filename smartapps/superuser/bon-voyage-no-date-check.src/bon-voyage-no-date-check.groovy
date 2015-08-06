/**
 *  Bon Voyage
 *
 *  Author: SmartThings
 *  Date: 2013-03-07
 *
 *  Monitors a set of presence detectors and triggers a mode change when everyone has left.
 *  
 */


// Automatically generated. Make future change here.
definition(
    name: "Bon Voyage - no date check",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Simpler Bon Voyage, to avoid state creation timestamp, which for iphones is affected by app usage",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("When all of these people leave home") {
		input "people", "capability.presenceSensor", multiple: true
	}
	section("Change to this mode") {
		input "awayMode", "mode", title: "Mode?"
	}
	section("When any of them comes back, set this mode") {
		input "backMode", "mode", title: "Mode?"
	}
	section("And text me at (optional)") {
		input "phone", "phone", title: "Phone number?", required: false
	}
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	subscribe(people, "presence", presence)
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
    unschedule()
	subscribe(people, "presence", presence)
    initialize()
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	if (evt.value == "not present") {
		if (location.mode != awayMode) {
			log.debug "checking if everyone is away"
			if (everyoneIsAway()) {
				log.debug "starting sequence"
				def delay = falseAlarmThreshold != null ? falseAlarmThreshold * 60 : 10 * 60
				runIn(delay, "takeAction")
			}
		}
		else {
			log.debug "mode is the same, not evaluating"
		}
	}
	else {
		log.debug "canceling"
		unschedule("takeAction")
        if (location.mode == awayMode) {
        	setLocationMode(backMode)
        }
	}
}

def takeAction()
{
	// TODO -- uncomment when app label is available
	//def message = "${app.label} changed your mode to '${newMode}' because everyone left home"
	def message = "Apparently everyone has left ${location.name}; mode is now set to '${awayMode}'"
	log.info message
//	sendPush(message)
	if (phone) {
		sendSms(phone, message)
	}
	setLocationMode(awayMode)
}

private initialize()
{
	if (everyoneIsAway()) {
    	if (location.mode != awayMode) {
    		setLocationMode(awayMode);
        }
    } else {
    	if (location.mode == awayMode) {
    		setLocationMode(backMode);
        }
   	}
}

private everyoneIsAway()
{
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}