(ns data-generator.core
  (:require [data-generator.constants :as const]))

(defn rand-from-vec [v]
  (get v (rand-int (count v))))

(defn last-name []
  (rand-from-vec const/last-names))

(defn male-first-name []
  (rand-from-vec const/male-first-names))

(defn female-first-name []
  (rand-from-vec const/female-first-names))

(defn first-name [gender]
  (if (= gender "male")
    (male-first-name)
    (female-first-name)))

(defn org-id []
  (str "org-" (rand-int (count const/org-names))))

(defn prac-id []
  (str "prac-" (rand-int 100)))

(defn birth-date []
  (str
   (+ 1920 (rand-int 100))
   "-"
   (rand-from-vec ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12"])
   "-"
   (rand-from-vec ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10"
                   "11" "12" "13" "14" "15" "16" "17" "18" "19" "20"
                   "21" "22" "23" "24" "25" "26" "27" "28" "29" "30" "31"])))

(defn male-or-female []
  (rand-from-vec ["male" "female"]))

(defn snomed-positive-or-negative []
  (rand-from-vec [{:system "http://snomed.info/sct"
                   :code "10828004"
                   :display "Positive"}
                  {:system "http://snomed.info/sct"
                   :code "260385009"
                   :display "Negative"}]))


(defn patient []
  (let [gender (male-or-female)]
    {:resourceType "Patient"
     :name {:given [(first-name gender)]
            :family (last-name)}
     :birthDate (birth-date)
     :gender gender
     :managingOrganization {:resourceType "Organization"
                            :id (org-id)}}))

(defn practitioner []
  (let [gender (male-or-female)]
    {:resourceType "Practitioner"
     :name {:given [(first-name gender)]
            :family (last-name)}
     :active true
     :birthDate (birth-date)
     :gender gender}))

(defn organizations []
  (map-indexed
   (fn [i org-name]
     {:name org-name
      :active true})
   const/org-names))

(defn period []
  (let [date (str
              (+ 2000 (rand-int 20))
              "-"
              (rand-from-vec ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12"])
              "-"
              (rand-from-vec ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10"
                              "11" "12" "13" "14" "15" "16" "17" "18" "19" "20"
                              "21" "22" "23" "24" "25" "26" "27" "28" "29" "30" "31"]))
        hours (rand-from-vec (mapv (fn [start end] {:start start :end end})
                                  ["00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10"
                                   "11" "12" "13" "14" "15" "16" "17" "18" "19" "20"
                                   "21" "22"]
                                  ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10"
                                   "11" "12" "13" "14" "15" "16" "17" "18" "19" "20"
                                   "21" "22" "23"]))]
    {:start (str date "T" (:start hours) ":00:00")
     :end (str date "T" (:end hours) ":00:00")}))

(defn encounter [pat-id]
  {:resourceType "Encounter"
   :class {:system "http://terminology.hl7.org/CodeSystem/v3-ActCode"
           :code (rand-from-vec ["AMB" "EMER" "FLD" "HH" "IMP" "ACUTE" "NONAC" "OBSENC" "PRENC" "SS" "VR"])}
   :subject {:resourceType "Patient"
             :id pat-id}
   :participant [{:individual {:resourceType "Practitioner" :id (prac-id)}}]
   :period (period)})

(defn observation [pat-id]
  {:resourceType "Observation"
   :subject {:resourceType "Patient"
             :id pat-id}
   :status "registered"
   :code {:coding [{:system "http://loinc.org"
                    :code "10331-7"
                    :display "Rh [Type] in Blood"}]}
   :valueCodeableConcept {:coding [(snomed-positive-or-negative)]}
   :effectiveDateTime ""})

(comment

  (encounter "pat-1")

  (patient)

  (observation "pat-1")

  (organizations)


  )
