(ns orc_battle_online.html_rendering
  (:use orc_battle_online.game_logic)
  (:use (hiccup core)))

(defmulti monster-show-html :type)
(defmethod monster-show-html :default [m]
	   (str "Health=" (:health m) ": A fierce " (:type m) " monster"))

(defn show-monsters-html []
  (html [:ol (map (fn [i m]
	 (html [:li.monster (if (monster-dead m)
	   "*dead*"
	   (monster-show-html m))]))
       (range 1 (inc (count @*monsters*)))
       @*monsters*)]))

(defn show-actions-html []
  (html [:a {:href "stab"} "Stab"]
	[:br]))

(defn show-player-html []
  (html [:p
	 (str "You are a valiant knight with a health of " @*player-health*
	      ", an agility of " @*player-agility*
	      ", and a strength of " @*player-strength*)]))

(defn render-game-html []
  (html [:div#player (show-player-html)]
	[:div#monsters (show-monsters-html)]
	[:div#actions (show-actions-html)]))
