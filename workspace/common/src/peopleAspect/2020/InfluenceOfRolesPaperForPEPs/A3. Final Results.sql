SELECT * 					-- clusterbysenderfullname,
	FROM (-- social discussion metrics
		SELECT author AS m, peptype, folder,
					COUNT(distinct pep) AS pepInvolvement, SUM(totalmsgs) AS TotalMsgByPerson, 	-- SUM(totalmsgs)/COUNT(pep) AS AVEG, 
					SUM(msgs_not_replies) AS TotalTopLevelMsgsByPerson, 	-- SUM(msgs_not_replies)/COUNT(pep) AS AVEG2, 
					SUM(msgs_are_replies) AS TotalRepliesByPersonToDiffMsgs, 	-- SUM(msgs_are_replies)/COUNT(pep) AS AVEG3,
					SUM(totalinreplyto) AS TotalRepliesToThisPerson 	-- SUM(totalinreplyto)/COUNT(pep) AS AVEG4,
					-- SUM(summessages) AS TotalMsgsForPEP -- , dominance_index
					from communicationsforseeders
				-- WHERE author IN (SELECT author FROM top10authors)
					WHERE (author IN (SELECT author FROM top10authors) OR author = 'proposalauthor'  OR author = 'bdfl_delegate' )
					GROUP BY author -- , peptype -- , folder -- , peptype -- , state -- , peptype, state
					ORDER BY pepInvolvement DESC, TotalMsgByPerson DESC
		) AS tbA
	-- We join with how many times this particular person has been replied to
							-- inReplyToUserUsingClusteredSender
	LEFT JOIN (	
		SELECT clusterbysenderfullname AS m,peptype2020,folder, COUNT(DISTINCT threadid) AS threadCNT  
			FROM pepthreadsbyperson2020 
			-- where threadlevel < 3  ...for firsta nd second level results
			GROUP BY clusterbysenderfullname, peptype2020
			-- ORDER BY threadCNT DESC
		UNION
			SELECT authorsrole AS m,peptype2020, folder, COUNT(DISTINCT threadid) AS threadCNT  
			FROM pepthreadsbyperson2020_PABD 
			-- where threadlevel < 3  ...for firsta nd second level results
			WHERE authorsrole = 'proposalAuthor' OR authorsrole = 'bdfl_delegate'
			GROUP BY authorsrole -- , peptype2020
		ORDER BY threadCNT DESC
		) as tbC
	-- ON tbA.clusterbysenderfullname = tbC.inReplyToUserUsingClusteredSender;
	ON tbA.m = tbC.m AND tbA.peptype = tbC.peptype2020
	ORDER BY tbA.pepInvolvement desc;

