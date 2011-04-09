(ns orc_battle_online.stateful_links
  (:use (hiccup core)))

(def *link-map* (ref {}))

(defn create-link [text fun]
  (dosync
   (alter *link-map* assoc (hash fun) fun)
   (html [:a {:href (hash fun)} text])))