(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware flash reload stacktrace keyword-params params session))
  (:use (ring.middleware.session memory))
  (:use ring.util.response)
  (:use (hiccup core form-helpers))
  (:use (orc_battle_online game_logic html_rendering stateful_links)))

(defmulti handler :uri)

(defmethod handler "/main" [req]
	   (response-html (str (render-game-html req)
			       (create-link "Random Fun"
					    (fn [req]
					      (response-html "Congrats! You have called a method"))))))

(defmethod handler "/newgame" [req]
	   (init-monsters)
	   (init-player)
	   (redirect "/main"))

(defmethod handler "/roundhouse" [req]
	   (let [output (with-out-str (with-in-str (str "r\r\n") (player-attack)))]
	   (-> (redirect "/main")
	       (assoc :flash output))))

(defmethod handler "/choose-double-swing-target" [req]
	   (response-html (html (show-monsters-html)
				(form-to [:post "/double-swing-attack"]
					 (label "target-one" "Monster #1")
					 (text-field "target-one")
					 (label "target-two" "Monster #2")
					 (text-field "target-two")
					 (submit-button "Submit")))))

(defmethod handler "/double-swing-attack" [req]
	   (let [output (with-out-str (with-in-str (str "d\r\n"
							(:target-one (:params req))
							"\r\n"
							(:target-two (:params req))
							"\r\n") (player-attack)))]
	     (-> (redirect "/main")
		 (assoc :flash output))))
	     

(defmethod handler "/stab" [req]
	   (response-html (html (show-monsters-html)
			       (form-to [:post "/stab-monster"]
					(label "stab-choice" "Which monster will you stab?")
					(text-field "stab-choice")
					(submit-button "Submit")))))


(defmethod handler "/stab-monster" [req]
	   (let [x (:stab-choice (:params req))]
	     (with-in-str (str "s\r\n" x "\r\n") (player-attack))
	     (-> (redirect "/main")
		 (assoc :flash (str "You stabbed monster " x)))))

(defmethod handler "/" [req]
	   (response-html (html [:h1 "Hello World from Ring and Hiccup!"]
				[:a {:href "newgame"} "New Game"])))

(defmethod handler :default [req]
	   (follow-link req))

(def app (-> handler
	     (wrap-flash)
	     (wrap-session {:cookie-attrs {:path "/"} :store (memory-store)})
	     (wrap-keyword-params)
	     (wrap-params)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (init-monsters)
  (init-player)
  (run-jetty app {:port 8080}))

(defn -main [&args]
  (boot))