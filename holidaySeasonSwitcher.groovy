/**
 *  Holiday Season Switcher
 *
 *  Copyright 2024 lnjustin
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 * V1.0 - Initial Release

 */

import java.text.SimpleDateFormat
import groovy.transform.Field

definition(
    name: "Holiday Season Switcher",
    namespace: "lnjustin",
    author: "Justin Leonard, Dominick Meglio",
    description: "Dynamically turn on switch between two holidays",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"

preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
	page name: "apiAccessPage", title: "Calendarific API Access", install: false, nextPage: "mainPage"
}

def mainPage() {
    dynamicPage(name: "mainPage") {

            section {
              //  header()
                paragraph getInterface("header", " Calendarific API")
                href(name: "apiAccessPage", title: getInterface("boldText", "Configure Calendarific API Access"), description: "API Access Required for Holiday Lookup", required: false, page: "apiAccessPage", image: (calAPIToken ? checkMark : xMark))
                
                input(name:"holidaySeasonSwitch", type: "switch", title: "Holiday Season Switch", required:true, multiple:false)
                
                if (!areHolidaysInState()) getHolidays()
                
                paragraph getInterface("header", " First Day of Holiday Season")
                if ((firstHolidayReligious == null || firstHolidayReligious.size() == 0) && (firstHolidayObservance == null || firstHolidayObservance.size() == 0)) input(name:"firstHolidayNational", type: "enum", title: "National Holidays", options:state.nationalHolidays, required:false, multiple:false)
			    if ((firstHolidayNational == null || firstHolidayNational.size() == 0) && (firstHolidayObservance == null || firstHolidayObservance.size() == 0))input(name:"firstHolidayReligious", type: "enum", title: "Religious Holidays", options:state.religiousHolidays, required:false, multiple:false)
			    if ((firstHolidayReligious == null || firstHolidayReligious.size() == 0) && (firstHolidayNational == null || firstHolidayNational.size() == 0))input(name:"firstHolidayObservance", type: "enum", title: "Observances", options:state.observanceHolidays, required:false, multiple:false)
                input("firstHolidayOffset", "number", title: "Offset (+/-) Days", defaultValue: 0, required: false)

                paragraph getInterface("header", " Last Day of Holiday Season")
                if ((lastHolidayReligious == null || lastHolidayReligious.size() == 0) && (lastHolidayObservance == null || lastHolidayObservance.size() == 0)) input(name:"lastHolidayNational", type: "enum", title: "National Holidays", options:state.nationalHolidays, required:false, multiple:false)
			    if ((lastHolidayNational == null || lastHolidayNational.size() == 0) && (lastHolidayObservance == null || lastHolidayObservance.size() == 0))input(name:"lastHolidayReligious", type: "enum", title: "Religious Holidays", options:state.religiousHolidays, required:false, multiple:false)
			    if ((lastHolidayReligious == null || lastHolidayReligious.size() == 0) && (lastHolidayNational == null || lastHolidayNational.size() == 0))input(name:"lastHolidayObservance", type: "enum", title: "Observances", options:state.observanceHolidays, required:false, multiple:false)
                input("lastHolidayOffset", "number", title: "Offset (+/-) Days", defaultValue: 0, required: false)                
                
           }
            section (getInterface("header", " Settings")) {
			    input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
		    }
            section("") {
                
                footer()
            }
    }
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center">&copy; 2024 lnjustin.<br>'
}

def apiAccessPage() {
	dynamicPage(name: "apiAccessPage", title: "Connect to Calendarific API", nextPage:"mainPage", uninstall:false, install: false) {
		section("API Access"){
			paragraph "You can obtain a free API key going to <a href='https://calendarific.com'>https://calendarific.com</a>"
			input("calAPIToken", "text", title: "API Key", description: "API Key", required: true)
			input("calCountry", "enum", title: "Country", description: "Country", options: countryList, required: true)
		}
	}
	
}

def areHolidaysInState() {
    def inState = true
    if (state.nationalHolidays == null || state.nationalHolidays == [:]) inState = false
    if (state.religiousHolidays == null || state.religiousHolidays == [:]) inState = false
    if (state.observanceHolidays == null || state.observanceHolidays == [:]) inState = false
    return inState
}

def areWithinDates(startMonth, startDay, stopMonth, stopDay) {
    def withinDates = false
    def today = timeToday(null, location.timeZone)
	def month = today.month+1
	def day = today.date
        
    if ((month == startMonth && day >= startDay) || month > startMonth)  {
        if ((month == stopMonth && day <= stopDay) || month < stopMonth) {
		     withinDates = true
        }
    }
    return withinDates
}

def installed() {
	initialize()
}

def updated() {
    unschedule()
	unsubscribe()
    resetState()
	initialize()
}

def uninstalled() {
    deleteChild()
	logDebug "Uninstalled app"
}

def initialize() {
    setDates()
    
    update()
    schedule("01 00 00 ? * *", update)	    
    
}

def setDates() {
    if (!areHolidaysInState()) getHolidays()
    
}

def update() {
	def today = timeToday(null, location.timeZone)
	def year = today.year+1900
	def month = today.month+1
	def day = today.date
	
	// Refresh the holidays for this year
	if (month == 1 && day == 1)
		getHolidays()
    
	setPreconfiguredHolidays(calNational, state.nationalHolidaysList, year, month, day)
	setPreconfiguredHolidays(calReligious, state.religiousHolidaysList, year, month, day)
	setPreconfiguredHolidays(calObservances, state.observanceHolidaysList, year, month, day)
}

def setPreconfiguredHolidays(holidayList, fulllist, year, month, day)
{
	for (holiday in holidayList)
	{
		def holidayDate = fulllist[holiday]
		if (holidayDate != null && holidayDate.year == year && holidayDate.month == month && holidayDate.day == day)
			state.activeHolidays.add(holiday)
	}

}

def extractHolidays(holidayList)
{
	def result = [:]
	
	for(holiday in holidayList)
	{
		result[holiday.name] = holiday.name
	}
	
	return result
}

def extractHolidayDetails(holidayList)
{
	def result = [:]
	
	for(holiday in holidayList)
	{
		result[holiday.name] = holiday.date.datetime
	}
	
	return result
}

def getHolidays()
{
	state.nationalHolidays = [:]
	state.religiousHolidays = [:]
	state.observanceHolidays = [:]
    
	state.nationalHolidaysList = [:]
	state.religiousHolidaysList = [:]
	state.observanceHolidaysList = [:]
    
    def result = sendApiRequest("national", "GET")
	if (result.status == 200) {
		state.nationalHolidays = extractHolidays(result.data.response.holidays)
		state.nationalHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("religious", "GET")
	if (result.status == 200) {
		state.religiousHolidays = extractHolidays(result.data.response.holidays)
		state.religiousHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("observance", "GET")
	if (result.status == 200) {
		state.observanceHolidays = extractHolidays(result.data.response.holidays)
		state.observanceHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
}

def getHolidayEnumLists() {
	def nationalHolidays = [:]
	def religiousHolidays = [:]
	def observanceHolidays = [:]
    
    def result = sendApiRequest("national", "GET")
	if (result.status == 200) {
		nationalHolidays = extractHolidays(result.data.response.holidays)
	}
		
	result = sendApiRequest("religious", "GET")
	if (result.status == 200) {
		religiousHolidays = extractHolidays(result.data.response.holidays)
	}
		
	result = sendApiRequest("observance", "GET")
	if (result.status == 200) {
		observanceHolidays = extractHolidays(result.data.response.holidays)
	}
    
    return [nationalHolidays: nationalHolidays, religiousHolidays: religiousHolidays, observanceHolidays: observanceHolidays]
}

def getHolidayDates() {    
	state.nationalHolidaysList = [:]
	state.religiousHolidaysList = [:]
	state.observanceHolidaysList = [:]
    
    def result = sendApiRequest("national", "GET")
	if (result.status == 200) {
		state.nationalHolidays = extractHolidays(result.data.response.holidays)
		state.nationalHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("religious", "GET")
	if (result.status == 200) {
		state.religiousHolidays = extractHolidays(result.data.response.holidays)
		state.religiousHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("observance", "GET")
	if (result.status == 200) {
		state.observanceHolidays = extractHolidays(result.data.response.holidays)
		state.observanceHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}    
}

def areHolidaysInState() {
    def inState = true
    if (state.nationalHolidays == null || state.nationalHolidays == [:]) inState = false
    if (state.religiousHolidays == null || state.religiousHolidays == [:]) inState = false
    if (state.observanceHolidays == null || state.observanceHolidays == [:]) inState = false
    return inState
}

def sendApiRequest(type, method)
{
    def params = [
		uri: "https://calendarific.com",
        path: "/api/v2/holidays",
		contentType: "application/json",
		query: [
                api_key: calAPIToken,
				country: calCountry,
				year: timeToday(null, location.timeZone).year + 1900,
				type: type
            ],
		timeout: 300
	]

    if (body != null)
        params.body = body
    
    def result = null
    if (method == "GET") {
        httpGet(params) { resp ->
            result = resp
        }
    }
    else if (method == "POST") {
        httpPost(params) { resp ->
            result = resp
        }
    }
    return result
}


def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}
    

def getInterface(type, txt="", link="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "error": 
            return "<div style='color:#ff0000;font-weight: bold;'>${txt}</div>"
            break
        case "note": 
            return "<div style='color:#333333;font-size: small;'>${txt}</div>"
            break
        case "subField":
            return "<div style='color:#000000;background-color:#ededed;'>${txt}</div>"
            break     
        case "subHeader": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "subSection1Start": 
            return "<div style='color:#000000;background-color:#d4d4d4;border: 0px solid'>"
            break
        case "subSection2Start": 
            return "<div style='color:#000000;background-color:#e0e0e0;border: 0px solid'>"
            break
        case "subSectionEnd":
            return "</div>"
            break
        case "boldText":
            return "<b>${txt}</b>"
            break
        case "link":
            return '<a href="' + link + '" target="_blank" style="color:#51ade5">' + txt + '</a>'
            break
    }
} 
