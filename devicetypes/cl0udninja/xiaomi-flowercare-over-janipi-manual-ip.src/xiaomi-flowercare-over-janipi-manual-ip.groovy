/**
 *  Raspberry Pi Temperature Sensor
 *
 *  Licensed under the GNU v3 (https://www.gnu.org/licenses/gpl-3.0.en.html)
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
preferences {		
	input("ip", "string", title:"IP Address", description: "192.168.1.150", defaultValue: "192.168.1.150" ,required: true, displayDuringSetup: true)		
	input("port", "string", title:"Port", description: "80", defaultValue: "80" , required: true, displayDuringSetup: true)		
}
metadata {
	definition (name: "Xiaomi FlowerCare over JaniPi(Manual IP)", namespace: "cl0udninja", author: "Janos Elohazi") {
        capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Battery"
   		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Illuminance Measurement"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	// UI tile definitions
	tiles(scale: 2) {
        valueTile("moisture", "device.humidity", inactiveLabel: false, decoration: "flat", width: 6, height: 4) {
			state "default", label:'${currentValue}%', icon:"st.Outdoor.outdoor5",
            backgroundColors:[
				[value: 0, color: "#725438"],
                [value: 14, color: "#96773d"],
				[value: 15, color: "#44b621"],
				[value: 60, color: "#9db621"],
				[value: 100, color: "#ff1e1e"]                                     
            ]
		}
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state "temperature", icon:"st.Weather.weather2",
            backgroundColors:[
				[value: 0, color: "#153591"],
				[value: 5, color: "#1e9cbb"],
				[value: 10, color: "#90d2a7"],
				[value: 15, color: "#44b621"],
				[value: 20, color: "#f1d801"],
				[value: 25, color: "#d04e00"],
				[value: 30, color: "#bc2323"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]                                      
            ]
		}
        valueTile("light", "device.light", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", icon:"st.Weather.weather14",
            backgroundColors:[
				[value: 0, color: "#777775"],
                [value: 3000, color: "#fca00c"],
                [value: 6000, color: "#fffd93"],
				[value: 100000, color: "#ffff8c"]
            ]
		}
        valueTile("fertility", "device.fertility", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", icon:"st.Outdoor.outdoor3",
            backgroundColors:[
				[value: 0, color: "#72471c"],
				[value: 350, color: "#e2cc24"],
				[value: 2000, color: "#73e224"],
				[value: 10000, color: "#e22424"]                                     
            ]
		}

		valueTile("temperatureText", "device.temperature", decoration: "flat", inactiveLabel: false, width: 2, height: 1) {
			state "default", label:'${currentValue}°'}
        valueTile("lightText", "device.light", decoration: "flat", inactiveLabel: false, width: 2, height: 1) {
			state "default", label:'${currentValue} lux'}
        valueTile("fertilityText", "device.fertility", decoration: "flat", inactiveLabel: false, width: 2, height: 1) {
			state "default", label:'${currentValue}  µS/cm'}
            
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 3, height: 1) {
			state "default", label:'${currentValue}% battery', unit:"", icon:"st.Appliances.appliances17"
		}
        valueTile("firmware", "device.firmware", decoration: "flat", inactiveLabel: false, width: 3, height: 1) {
			state "default", label:'v${currentValue}', unit:""
		}
        valueTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 2) {
			state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }            
		main(["moisture"])
		details(["moisture",
        		 "temperature", "light", "fertility",
                 "temperatureText", "lightText", "fertilityText",
                 "battery", "refresh",  "firmware"])
    }
}

def installed() {
	log.debug "installed"
	updated()
}

def updated() {
	log.debug "updated"
	initialize();
}

def ping() {
	log.debug "ping"
	poll()
}

def initialize() {
	log.debug "initialize"
    unschedule()
    runEvery1Hour(refresh)
    refresh()
}

// parse events into attributes
def parse(description) {
	log.debug "Parse ${description}"
    if (!description.hasProperty("body")) {
    	log.debug "Skipping parse"
        return
    }
    log.debug "Parsing '${description?.body}'"
	def msg = parseLanMessage(description?.body)
    log.debug "Msg ${msg}"
	def json = parseJson(description?.body)
    log.debug "JSON '${json}'"
    
    if (getTemperatureScale() == "C") {
    	sendEvent(name: "temperature", value: json.temperatureC)
    } else {
    	sendEvent(name: "temperature", value: json.temperatureC * 9 / 5 + 32)
    }
    sendEvent(name: "humidity", value: json.moisture)
    sendEvent(name: "light", value: json.light)
    sendEvent(name: "fertility", value: json.fertility)
    sendEvent(name: "battery", value: json.batteryPercent)
    sendEvent(name: "firmware", value: json.firmwareVersion)
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    getTemp()
}

def refresh() {
	log.debug "Executing 'refresh'"
    getTemp()
}

private getTemp() {
	def iphex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)

    def uri = "/api/flower"
    def headers=[:]
    headers.put("HOST", "${ip}:${port}")
    headers.put("Accept", "application/json")
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: uri,
		headers: headers,
        "${ipHex}:${portHex}",
        [callback: parse]
    )
    log.debug "Getting FlowerCare data ${hubAction}"
    hubAction
}

private String convertIPtoHex(ipAddress) {
	log.debug "convertIPtoHex ${ipAddress} to hex"
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	log.debug "convertPortToHex ${port} to hex"
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}