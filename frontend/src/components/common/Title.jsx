import React from 'react'
import SubTitle from './SubTitle';
import Clock from './Clock';
import Note from './Note';

const notes = [
    {
        title: "오늘 할 일",
        content: "맛깔나게 숨쉬기"
    },
    {
        title: "자고쉽다",
        content: "자야징"
    }
]

function Title(props) {
  return (
    <div>
        <h1>{`제목`}</h1>
        <SubTitle title={"고라니"}/>
        <Clock />
        {notes.map((notes) => (
            <Note title={notes.title} content={notes.content} />
        ))}
    </div>
    )
}

export default Title;