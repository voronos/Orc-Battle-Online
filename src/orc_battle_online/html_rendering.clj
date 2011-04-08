(ns orc_battle_online.html_rendering
  (:use orc_battle_online.game_logic)
  (:use (hiccup core page-helpers)))

(defn monster-show-html [m]
  (str (with-out-str (monster-show m)) " (Health: " (:health m) ")"))

(defn show-monsters-html []
  (html (ordered-list (map (fn [m]
	 (if (monster-dead m)
	   "*dead*"
	   (monster-show-html m)))
       @*monsters*))))

(defn show-actions-html []
  (html [:a {:href "stab"} "Stab"]
	[:br]
	[:a {:href "roundhouse"} "Roundhouse"]
	[:br]
	[:a {:href "choose-double-swing-target"} "Double swing"]))

(defn show-player-html []
  (html [:p
	 (str "You are a valiant knight with a health of " @*player-health*
	      ", an agility of " @*player-agility*
	      ", and a strength of " @*player-strength*)]))

(defn render-game-html [req]
  (html4 [:body
	  (if (:flash req) [:div#flash [:pre (:flash req)]])
	  ;(if (get-in req [:session :_flash]) [:div#_flash (get-in req [:session :_flash])])
	  [:div#player (show-player-html)]
	  [:div#monsters (show-monsters-html)]
	  [:div#actions (show-actions-html)]]))
