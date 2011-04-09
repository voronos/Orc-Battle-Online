(ns orc_battle_online.stateful_links
  (:use (hiccup core)))

(def *link-map* (ref {}))
;; TODO figure out a way to clean up the link-map from time to time
;; TODO add validation function to ensure that only functions with an arity of one are added as values
(defn create-link [text fun]
  (dosync
   (alter *link-map* assoc (hash fun) fun)
   (html [:a {:href (hash fun)} text])))

(defn follow-link [request-map]
  ((get @*link-map* (Integer/parseInt (apply str (rest (:uri request-map))))) request-map))