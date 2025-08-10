import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekCardComponent } from './user-gameweek-card.component';

describe('UserGameweekCardComponent', () => {
  let component: UserGameweekCardComponent;
  let fixture: ComponentFixture<UserGameweekCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
