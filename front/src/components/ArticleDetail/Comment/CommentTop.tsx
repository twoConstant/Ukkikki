import React from "react";
import editBtn from "@/assets/ArticleDetail/edit.png";
import deleteBtn from "@/assets/ArticleDetail/delete.png";
import { CommentTopInterface } from "../../../types/Comment/CommentInterface";

const CommentTop: React.FC<CommentTopInterface> = ({ comment, isModify, commentDelete, switchIsModify, createReply}) => {
	return (
		<div className="flex justify-between">
			<div className="flex gap-2">
				<div className="font-pre-SB text-black text-xs">{comment.userName}</div>
				<div className="font-pre-L text-point-gray text-[10px]">
					{comment.createdDate}
				</div>
			</div>

			{!isModify && (
				<div className="flex gap-2">
					<img src={editBtn} className="w-4" onClick={switchIsModify} />
					<img src={deleteBtn} className="w-3" onClick={commentDelete} />
					<span className="text-blue-500 text-xs" onClick={createReply}>답글달기</span>
				</div>
			)}
		</div>
	);
};

export default CommentTop;
