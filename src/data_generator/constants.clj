(ns data-generator.constants
  (:require [clojure.string :as str]))

(def last-names (mapv str/capitalize (str/split (slurp "resources/last-names.txt") #"\n")))

(def male-first-names (mapv str/capitalize (str/split (slurp "resources/male-first-names.txt") #"\n")))

(def female-first-names (mapv str/capitalize (str/split (slurp "resources/female-first-names.txt") #"\n")))

(def org-names (mapv str/capitalize (str/split (slurp "resources/org-names.txt") #"\n")))
